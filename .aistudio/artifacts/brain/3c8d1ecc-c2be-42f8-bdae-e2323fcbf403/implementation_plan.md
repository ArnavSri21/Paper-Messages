# PaperMessages — Startup Architecture & Technical Implementation Plan

PaperMessages is a startup-grade, privacy-first communication and community platform built around **advanced threaded group conversations** and a signature **Liquid Glass × Deep Dark × Fluid Motion** design language. This revised specification transitions the architecture into a **Modular Monolith** engineered for rapid startup iteration, cryptographic integrity, sustainable unit economics, and zero-rewrite horizontal growth.

---

## User Review & Unresolved Technical Decisions

> [!IMPORTANT]
> Review the following architectural choices and unresolved technical decisions before Milestone 1 implementation begins:

- **Confirmed Core Decisions (From Discovery)**:
  - **Identity & Auth**: Firebase Authentication with Google Sign-In (Credential Manager) + Phone OTP verification step and mandatory `@username` handle registration.
  - **First-Launch Onboarding**: Pre-seeded starter communities, channels, topics, and multi-reply threads so users can immediately experience structured group discussions.
  - **Calling & Media**: Real-time signaling paired with CameraX video preview, audio waveform visualization, and STUN/TURN/SFU call orchestration.
  - **Cryptographic Foundation**: Hardware-backed Android Keystore identity keys, authenticated end-to-end encryption for private chats and attachments, and transparent security indicators.
- **Unresolved Technical Decision 1 — Primary Backend Runtime (`Kotlin/Ktor` vs. `TypeScript/NestJS`)**:
  - *Recommended Default*: **Kotlin + Ktor Modular Monolith**, enabling shared domain serialization models, validation rules, and cryptographic DTOs with the Android client while keeping memory footprint low (~150–250 MB per container).
- **Unresolved Technical Decision 2 — Group Encryption Threshold (Sender Keys vs. MLS RFC 9420)**:
  - *Recommended Default*: **Signal Double Ratchet (X3DH)** for 1:1 chats, **MLS (RFC 9420)** for private encrypted groups up to 1,000 members (`O(log N)` key updates), and **Server-Side TLS + At-Rest Encryption** (explicitly labeled in UI as non-E2EE) for public discoverable communities and broadcast channels where history must be immediately searchable by new joiners.
- **Unresolved Technical Decision 3 — Android Runtime Execution Bridge**:
  - *Recommended Default*: Implement strict domain Repository interfaces on Android (`AuthRepository`, `ConversationRepository`, `ThreadRepository`, `CommunityRepository`, `CallRepository`, `CryptoEngine`) backed by **Room (SQLite FTS4 local cache & offline queue)** and synchronized in real time via the provisioned **Firebase Auth + Enterprise Firestore** cloud backend in the AI Studio environment, with clean adapter boundaries ready to point to the Ktor REST/WebSocket deployment.

---

## 1. Overview, Core Product Identity & Visual Design System

### 1.1 Product Vision & Signature Differentiator
- **Core Philosophy**: *"Conversations deserve more than a message feed. They deserve structure, space and clarity."*
- **Signature Experience — Threaded Group Conversations**: Every group conversation supports horizontal **Topics** (*General, Announcements, Resources, Questions*) and per-message **Threads** with denormalized live counters (`"12 replies · View thread"`), thread-level subscriptions/muting, `@mentions`, chronological replies, unread indicators, and instant jump-to-parent navigation. On phones (`Compact`), threads open with a connected full-screen slide transition; on tablets/foldables (`Expanded`), threads dock as a synchronized right-hand split panel.

### 1.2 Liquid Glass Visual Identity & Theme Tokens
- **Color Palette**:
  - **Main Background**: `#080A10` | **Secondary Background**: `#0D1019` | **Elevated Surface**: `#141925`
  - **Glass Fill & Specular Border**: `rgba(255, 255, 255, 0.055)` surface with `rgba(255, 255, 255, 0.11)` 1dp border
  - **Primary Brand Gradient**: Electric Violet (`#8B7CFF`) $\rightarrow$ Cobalt Blue (`#547BFF`)
  - **Secondary Ambient Gradient**: Luminous Cyan (`#54D6E8`) $\rightarrow$ Soft Violet (`#9B75FF`) | **Highlight**: `#B7A9FF`
  - **Typography Hierarchy (Bundled `Inter` Font Family)**:
    - Main Page Title: `30sp` SemiBold | Section Heading: `21sp` SemiBold | Conversation Title: `18sp` Medium
    - Message Body: `15sp` Regular (`22sp` line height) | Secondary Info: `12.5sp` Medium | Timestamps: `11sp` Medium
  - **Semantic Status**: Primary Text `#F5F6FC`, Secondary `#A0A6B8`, Muted `#697184`, Success `#43D9A3`, Warning `#FFBE69`, Error `#FF667D`
- **Three-Tier Reusable Liquid Glass Component System**:
  - **Glass Level 1 (Subtle)**: Topic pills, reaction chips, encryption badges (`4%` white luminance, `8%` border, zero text blur).
  - **Glass Level 2 (Standard)**: Floating capsule bottom navigation (`28dp` radius), floating message composer, search bar, pinned chat cards (`6%` white luminance, `12%` border highlight, soft backdrop blur with solid fallback on low-RAM devices).
  - **Glass Level 3 (Elevated)**: Thread panels, contextual long-press menus, profile sheets, call control dock (`9%` white luminance over `#141925`, `16%` directional border highlight, deep ambient elevation).
- **Shape & Motion Rules**:
  - Card radius `20dp`, Message bubble radius `18dp`, Modal radius `28dp`, Floating Nav `28dp`, Buttons `16dp`, `48dp` minimum touch targets.
  - Natural spring physics (`dampingRatio = 0.82f`, `stiffness = 400f`) with a global **Reduced Motion** accessibility toggle.

---

## 2. System Architecture & Modular Monolith Design

### 2.1 Why a Modular Monolith First
Instead of premature microservices (which introduce distributed transaction overhead, service mesh complexity, and high baseline cloud costs), PaperMessages uses a **strictly partitioned Modular Monolith**. Each domain module owns its database tables, internal service layer, and API routes, communicating with sibling modules exclusively through in-process domain interfaces and an internal event bus. High-throughput modules (such as the **WebSocket Real-Time Gateway** or **Calling Signaling**) can later be extracted into standalone deployments without altering domain logic.

### 2.2 Updated System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID CLIENT (Kotlin + Jetpack Compose)                   │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │  UI Layer: Liquid Glass System • Chats • Group Topics & Threads • Calls UI   │  │
│  └──────────────────────────────────────┬───────────────────────────────────────┘  │
│                                         ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────────────────┐  │
│  │  Presentation Layer (MVVM): StateFlows • Thread Coordinator • Call Manager   │  │
│  └───────────────┬──────────────────────────────────────────────┬───────────────┘  │
│                  ▼                                              ▼                  │
│  ┌──────────────────────────────────────┐      ┌────────────────────────────────┐  │
│  │ Local Persistence & Offline Engine   │      │ Client Crypto Engine           │  │
│  │ • Room SQLite + FTS4 Encrypted Index │      │ • Android Keystore Identity    │  │
│  │ • Outbox Queue & Cursor Sync Engine  │      │ • X3DH / Double Ratchet / MLS  │  │
│  └───────────────┬──────────────────────┘      └────────────────┬───────────────┘  │
└──────────────────┼──────────────────────────────────────────────┼──────────────────┘
                   │ HTTPS REST (Idempotent) + WSS Real-Time      │ E2EE Ciphertext
                   ▼                                              ▼
┌────────────────────────────────────────────────────────────────────────────────────┐
│                     CLOUD EDGE & API GATEWAY (TLS 1.3 / WAF)                       │
└──────────────────┬──────────────────────────────────────────────┬──────────────────┘
                   ▼                                              ▼
┌─────────────────────────────────────────────────────────┐ ┌────────────────────────┐
│           PAPERMESSAGES MODULAR MONOLITH                │ │  MANAGED MEDIA & RTC   │
│  ┌────────────────┐ ┌────────────────┐ ┌─────────────┐  │ │  ┌──────────────────┐  │
│  │ 1. Auth &      │ │ 2. Profiles &  │ │ 3. Private  │  │ │  │ WebRTC SFU       │  │
│  │    Identity    │ │    Discovery   │ │    Messaging│  │ │  │ (STUN/TURN/SFU)  │  │
│  ├────────────────┤ ├────────────────┤ ├─────────────┤  │ │  └──────────────────┘  │
│  │ 4. Group       │ │ 5. Thread      │ │ 6. Community│  │ │  ┌──────────────────┐  │
│  │    Messaging   │ │    Management  │ │    Engine   │  │ │  │ Object Storage   │  │
│  ├────────────────┤ ├────────────────┤ ├─────────────┤  │ │  │ (Encrypted Blobs)│  │
│  │ 7. Broadcast   │ │ 8. File & Media│ │ 9. Push &   │  │ │  └──────────────────┘  │
│  │    Channels    │ │    Coordinator │ │    Presence │  │ │  ┌──────────────────┐  │
│  ├────────────────┤ ├────────────────┤ ├─────────────┤  │ │  │ Firebase Auth &  │  │
│  │10. Call        │ │11. Moderation &│ │12. Security │  │ │  │ FCM Push Gateway │  │
│  │    Signaling   │ │    Abuse Admin │ │    & Devices│  │ │  └──────────────────┘  │
│  └────────────────┘ └────────────────┘ └─────────────┘  │ └────────────────────────┘
└──────────────────┬──────────────────────────────────────┴───────┬──────────────────┘
                   ▼                                              ▼
┌─────────────────────────────────────────┐    ┌─────────────────────────────────────┐
│   PRIMARY RELATIONAL STORE (PostgreSQL) │    │   IN-MEMORY EPHEMERAL LAYER (Redis) │
│ • Normalized Schema (UUIDv7 PKs)        │    │ • WebSocket Session Routing         │
│ • Denormalized Thread Summaries         │    │ • Presence, Typing & Rate Limiting  │
│ • Read Watermarks & Audit Logs          │    │ • Pub/Sub Multi-Instance Fan-Out    │
└─────────────────────────────────────────┘    └─────────────────────────────────────┘
```

### 2.3 Module Boundaries & Responsibilities

| Module | Core Responsibility | Owned Tables / State |
|---|---|---|
| **1. Auth & Identity** | Validates Firebase ID tokens, manages phone OTP challenges, enforces session revocation. | `users_private`, `auth_sessions` |
| **2. Profiles & Discovery** | Manages public `@username` handles, display names, bios, privacy flags, and directory search. | `users_public`, `privacy_settings`, `user_blocks` |
| **3. Private Messaging** | Routes 1:1 E2EE envelopes, delivery ACKs, read watermarks, and disappearing timers. | `conversations`, `conversation_members`, `messages` |
| **4. Group Messaging** | Manages group roles, member permissions, topics, pinned messages, polls, and resource libraries. | `group_topics`, `polls`, `poll_votes`, `pinned_items` |
| **5. Thread Management** | Orchestrates parent-to-thread lifecycle, atomic reply counters, thread subscriptions, and thread unread state. | `thread_replies`, `thread_subscriptions` |
| **6. Community Management** | Handles public/private communities, category discovery, invite links, and linked groups/channels. | `communities`, `community_invites` |
| **7. Channels** | Enforces admin-only broadcast posting, subscriber counters, post reactions, and channel polls. | `channels`, `channel_subscribers` |
| **8. File & Media** | Issues short-lived pre-signed upload/download URLs, validates MIME/size quotas, tracks shared media. | `attachments` |
| **9. Notifications & Presence** | Tracks online/typing state in Redis and dispatches wake-up data pushes via FCM. | Redis `presence:*`, `fcm_tokens` |
| **10. Calling & Signaling** | Exchanges WebRTC SDP/ICE payloads, issues SFU room tokens, and logs call history. | `call_records` |
| **11. Moderation & Admin** | Processes user/message/group abuse reports, spam rate-limits, suspensions, and audit logs. | `moderation_reports`, `admin_audit_logs`, `feature_flags` |
| **12. Security & Devices** | Stores public identity keys, pre-key bundles, MLS key packages, and linked multi-device registry. | `user_devices`, `e2ee_prekeys`, `mls_key_packages` |

---

## 3. Detailed Normalized Database Schema (PostgreSQL)

All primary keys use **UUIDv7** (time-ordered UUIDs) for index locality and cursor pagination without leaking sequential integer counts. Private PII is strictly separated from publicly discoverable profile rows.

```sql
-- 1. USERS: Split Public Profile vs. Private PII
CREATE TABLE users_public (
    user_id         UUID PRIMARY KEY,
    username         VARCHAR(32) NOT NULL UNIQUE CHECK (username ~ '^[a-z0-9_]{3,32}$'),
    display_name    VARCHAR(64) NOT NULL,
    bio             VARCHAR(240) NOT NULL DEFAULT '',
    avatar_url       TEXT,
    avatar_color    VARCHAR(16) NOT NULL DEFAULT '#8B7CFF',
    status_text     VARCHAR(80) NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_public_username_trgm ON users_public USING gin (username gin_trgm_ops);
CREATE INDEX idx_users_public_display_trgm ON users_public USING gin (display_name gin_trgm_ops);

CREATE TABLE users_private (
    user_id              UUID PRIMARY KEY REFERENCES users_public(user_id) ON DELETE CASCADE,
    firebase_uid         VARCHAR(128) NOT NULL UNIQUE,
    phone_e164           VARCHAR(20) UNIQUE,
    phone_hash_sha256    CHAR(64) UNIQUE,
    email                VARCHAR(255),
    account_status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DELETED
    privacy_online       VARCHAR(16) NOT NULL DEFAULT 'EVERYONE', -- EVERYONE, CONTACTS, NOBODY
    privacy_last_seen    VARCHAR(16) NOT NULL DEFAULT 'EVERYONE',
    privacy_read_receipt BOOLEAN NOT NULL DEFAULT TRUE,
    privacy_phone_lookup VARCHAR(16) NOT NULL DEFAULT 'CONTACTS',
    privacy_group_invite VARCHAR(16) NOT NULL DEFAULT 'EVERYONE',
    last_seen_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. DEVICES & E2EE KEY DIRECTORY
CREATE TABLE user_devices (
    device_id            UUID PRIMARY KEY,
    user_id              UUID NOT NULL REFERENCES users_public(user_id) ON DELETE CASCADE,
    device_name          VARCHAR(64) NOT NULL,
    identity_key_pub     BYTEA NOT NULL,
    signed_prekey_id     INT NOT NULL,
    signed_prekey_pub    BYTEA NOT NULL,
    signed_prekey_sig    BYTEA NOT NULL,
    registration_id      INT NOT NULL,
    fcm_token            TEXT,
    last_active_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     revoked_at          TIMESTAMPTZ
);
CREATE INDEX idx_user_devices_user ON user_devices(user_id) WHERE revoked_at IS NULL;

-- 3. COMMUNITIES & CHANNELS
CREATE TABLE communities (
    community_id    UUID PRIMARY KEY,
    name            VARCHAR(80) NOT NULL,
    handle          VARCHAR(40) NOT NULL UNIQUE,
    description     VARCHAR(500) NOT NULL DEFAULT '',
    category        VARCHAR(40) NOT NULL DEFAULT 'Technology',
    avatar_url      TEXT,
    is_public       BOOLEAN NOT NULL DEFAULT TRUE,
    owner_id        UUID NOT NULL REFERENCES users_public(user_id),
    member_count    INT NOT NULL DEFAULT 1,
    invite_code     VARCHAR(24) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. CONVERSATIONS, TOPICS & MEMBERSHIPS
CREATE TABLE conversations (
    conversation_id      UUID PRIMARY KEY,
    community_id         UUID REFERENCES communities(community_id) ON DELETE SET NULL,
    type                 VARCHAR(16) NOT NULL, -- PRIVATE, GROUP, CHANNEL
    title                VARCHAR(100),
    description          VARCHAR(500) NOT NULL DEFAULT '',
    avatar_url           TEXT,
    encryption_mode      VARCHAR(16) NOT NULL, -- E2EE_SIGNAL, E2EE_MLS, TRANSPORT_TLS
    is_public            BOOLEAN NOT NULL DEFAULT FALSE,
    invite_code          VARCHAR(24) UNIQUE,
    last_message_id      UUID,
    last_message_preview VARCHAR(200),
    last_message_at      TIMESTAMPTZ,
    created_by           UUID NOT NULL REFERENCES users_public(user_id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE group_topics (
    topic_id        UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    name            VARCHAR(48) NOT NULL, -- e.g., General, Announcements, Resources, Questions
    icon_name       VARCHAR(32) NOT NULL DEFAULT 'tag',
    sort_order      INT NOT NULL DEFAULT 0,
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (conversation_id, name)
);

CREATE TABLE conversation_members (
    conversation_id          UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    user_id                  UUID NOT NULL REFERENCES users_public(user_id) ON DELETE CASCADE,
    role                     VARCHAR(16) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MODERATOR, MEMBER
    is_pinned                BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted                 BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived              BOOLEAN NOT NULL DEFAULT FALSE,
    marked_unread            BOOLEAN NOT NULL DEFAULT FALSE,
    last_read_message_id     UUID,
    last_read_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unread_count             INT NOT NULL DEFAULT 0,
    joined_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX idx_conv_members_user ON conversation_members(user_id, is_archived, is_pinned);

-- 5. MESSAGES (With Denormalized Thread Summaries)
CREATE TABLE messages (
    message_id               UUID PRIMARY KEY, -- Client-generated UUIDv7 for idempotency
    conversation_id          UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    topic_id                 UUID REFERENCES group_topics(topic_id) ON DELETE SET NULL,
    sender_id                UUID NOT NULL REFERENCES users_public(user_id),
    sender_device_id         UUID REFERENCES user_devices(device_id),
    client_dedup_key         UUID NOT NULL UNIQUE,
    message_type             VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT, ATTACHMENT, VOICE, POLL, SYSTEM
    ciphertext_payload       BYTEA, -- Populated for E2EE conversations
    iv_nonce                 BYTEA,
    plaintext_content        TEXT,  -- Populated ONLY for public groups/channels where TRANSPORT_TLS is used
    reply_to_message_id      UUID REFERENCES messages(message_id) ON DELETE SET NULL,
    is_pinned                BOOLEAN NOT NULL DEFAULT FALSE,
    is_edited                BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted               BOOLEAN NOT NULL DEFAULT FALSE,
    -- Denormalized Thread Summary Fields (Zero N+1 Query Timeline Rendering)
    thread_reply_count       INT NOT NULL DEFAULT 0,
    thread_last_reply_at     TIMESTAMPTZ,
    thread_last_sender_name  VARCHAR(64),
    thread_last_preview      VARCHAR(160),
    thread_participant_ids   UUID[] NOT NULL DEFAULT '{}',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_conv_cursor ON messages(conversation_id, message_id DESC);
CREATE INDEX idx_messages_topic_cursor ON messages(conversation_id, topic_id, message_id DESC);

-- 6. THREAD REPLIES & SUBSCRIPTIONS
CREATE TABLE thread_replies (
    reply_id                 UUID PRIMARY KEY,
    parent_message_id        UUID NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
    conversation_id          UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    sender_id                UUID NOT NULL REFERENCES users_public(user_id),
    quoted_reply_id          UUID REFERENCES thread_replies(reply_id) ON DELETE SET NULL,
    client_dedup_key         UUID NOT NULL UNIQUE,
    ciphertext_payload       BYTEA,
    iv_nonce                 BYTEA,
    plaintext_content        TEXT,
    mentioned_user_ids       UUID[] NOT NULL DEFAULT '{}',
    is_edited                BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted               BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_thread_replies_parent_cursor ON thread_replies(parent_message_id, reply_id ASC);

CREATE TABLE thread_subscriptions (
    parent_message_id        UUID NOT NULL REFERENCES messages(message_id) ON DELETE CASCADE,
    user_id                  UUID NOT NULL REFERENCES users_public(user_id) ON DELETE CASCADE,
    status                   VARCHAR(16) NOT NULL DEFAULT 'FOLLOWING', -- FOLLOWING, MUTED
    last_read_reply_id       UUID,
    unread_reply_count       INT NOT NULL DEFAULT 0,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (parent_message_id, user_id)
);

-- 7. REACTIONS, ATTACHMENTS, CALLS & MODERATION REPORTS
CREATE TABLE message_reactions (
    target_id       UUID NOT NULL, -- message_id or reply_id
    target_type     VARCHAR(12) NOT NULL, -- MESSAGE, THREAD_REPLY
    user_id         UUID NOT NULL REFERENCES users_public(user_id) ON DELETE CASCADE,
    emoji           VARCHAR(16) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (target_id, user_id, emoji)
);

CREATE TABLE attachments (
    attachment_id        UUID PRIMARY KEY,
    conversation_id      UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    uploader_id          UUID NOT NULL REFERENCES users_public(user_id),
    object_key           TEXT NOT NULL UNIQUE,
    file_name            VARCHAR(255) NOT NULL,
    mime_type            VARCHAR(100) NOT NULL,
    size_bytes           BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 104857600),
    encrypted_file_key   BYTEA, -- E2EE envelope for file AES-256-GCM key + SHA-256 digest
    sha256_ciphertext    CHAR(64) NOT NULL,
    duration_ms          INT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE call_records (
    call_id         UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    initiator_id    UUID NOT NULL REFERENCES users_public(user_id),
    call_type       VARCHAR(12) NOT NULL, -- VOICE, VIDEO
    status          VARCHAR(16) NOT NULL, -- RINGING, ACTIVE, ENDED, MISSED, DECLINED
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ,
    duration_sec    INT NOT NULL DEFAULT 0
);

CREATE TABLE moderation_reports (
    report_id       UUID PRIMARY KEY,
    reporter_id     UUID NOT NULL REFERENCES users_public(user_id),
    target_type     VARCHAR(16) NOT NULL, -- USER, MESSAGE, THREAD_REPLY, GROUP, COMMUNITY
    target_id       UUID NOT NULL,
    reason_category VARCHAR(32) NOT NULL, -- SPAM, HARASSMENT, ILLEGAL, IMPERSONATION, OTHER
    details         VARCHAR(1000) NOT NULL DEFAULT '',
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN', -- OPEN, REVIEWED, ACTIONED, DISMISSED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.1 Unread & Read-Receipt Strategy (Avoiding Write Amplification)
Instead of writing a separate row per recipient for every message sent in a 500-person group:
1. Each member's `conversation_members.last_read_message_id` and `last_read_at` watermark is updated via a debounced `READ_ACK` event when they view the timeline.
2. Reading a message with UUIDv7 `M_x` implicitly marks all messages `<= M_x` as read by that member.
3. Group read counts (`"Read by 18 members"`) are computed by counting members whose `last_read_message_id >= message_id` (respecting each member's `privacy_read_receipt` setting).

---

## 4. API & Real-Time WebSocket Specification

### 4.1 REST API Endpoints (`/api/v1`)

| Domain | Method & Path | Description |
|---|---|---|
| **Auth & Identity** | `POST /v1/auth/session` | Exchanges Firebase ID token + device public keys for session JWT & device registration. |
| **Auth & Identity** | `POST /v1/auth/phone/verify` | Verifies phone OTP and binds hashed phone number (`phone_hash_sha256`) to user identity. |
| **Profiles** | `GET /v1/users/search?q=` | Searches public profiles by `@username`, display name, or permitted phone hash. |
| **Profiles** | `PATCH /v1/users/me` | Updates display name, `@username`, bio, avatar, and granular privacy flags. |
| **Keys & Devices** | `GET /v1/keys/{userId}/bundle` | Fetches X3DH pre-key bundle / MLS KeyPackages for initiating E2EE sessions. |
| **Keys & Devices** | `POST /v1/devices/rotate-keys` | Uploads rotated signed pre-keys and one-time pre-keys; revokes stale device sessions. |
| **Conversations** | `GET /v1/conversations` | Cursor-paginated list of user's active, pinned, and archived conversations. |
| **Conversations** | `POST /v1/conversations` | Creates a 1:1 chat or group with default topics (`General`, `Announcements`, `Resources`, `Questions`). |
| **Messages** | `GET /v1/conversations/{id}/messages?before={uuid}&topicId={id}&limit=50` | Cursor-paginated messages with embedded denormalized thread summary metadata. |
| **Threads** | `GET /v1/messages/{parentId}/replies?after={uuid}&limit=50` | Fetches chronological thread replies and parent context. |
| **Threads** | `PUT /v1/messages/{parentId}/subscription` | Sets thread state (`FOLLOWING` or `MUTED`) and resets thread unread counter. |
| **Communities** | `GET /v1/communities/discover?category=` | Lists public communities, subscriber counts, and pending invite links. |
| **Attachments** | `POST /v1/attachments/presign-upload` | Validates MIME/size and returns a 10-minute pre-signed PUT URL for encrypted blob upload. |
| **Attachments** | `GET /v1/attachments/{id}/presign-download` | Verifies conversation membership and returns a 5-minute signed GET URL. |
| **Calls** | `POST /v1/calls/{conversationId}/initiate` | Creates call record, issues WebRTC TURN/SFU credentials, and triggers high-priority push. |
| **Moderation** | `POST /v1/moderation/reports` | Submits an abuse report (with optional message media key disclosure by victim for E2EE abuse). |

### 4.2 WebSocket Real-Time Protocol (`wss://rt.papermessages.app/v1/stream`)

All frames use a typed JSON or CBOR envelope with `eventId`, `type`, `correlationId`, and `payload`:

- **Client $\rightarrow$ Server Events**:
  - `msg.send`: `{ clientDedupKey, conversationId, topicId?, ciphertextPayload, ivNonce, attachmentIds[], mentions[] }`
  - `thread.reply`: `{ clientDedupKey, parentMessageId, conversationId, quotedReplyId?, ciphertextPayload, mentions[] }`
  - `msg.react`: `{ targetId, targetType, emoji, action: "ADD" | "REMOVE" }`
  - `conv.read_ack`: `{ conversationId, lastReadMessageId }`
  - `typing.state`: `{ conversationId, parentMessageId?, isTyping: boolean }`
  - `call.signal`: `{ callId, targetDeviceId, signalType: "OFFER" | "ANSWER" | "ICE_CANDIDATE" | "HANGUP", sdpOrCandidate }`
  - `sync.resume`: `{ lastReceivedEventId, knownConversationWatermarks }`
- **Server $\rightarrow$ Client Events**:
  - `msg.ack`: `{ clientDedupKey, serverMessageId, createdAt, status: "PERSISTED" }`
  - `msg.new`: Delivers new conversation message to online participants.
  - `thread.reply.new`: Delivers reply to open thread viewers AND emits `thread.summary.updated` (`{ parentMessageId, replyCount, lastReplyPreview, lastReplyAt }`) to the main group timeline.
  - `conv.receipt.updated`: `{ conversationId, userId, lastReadMessageId, readAt }` (omitted if user disabled read receipts).
  - `presence.updated`: `{ userId, isOnline, lastSeenAt? }`

**Idempotency & Offline Reconnection Guarantee**:
Every outgoing message or thread reply generates a deterministic `client_dedup_key` (UUIDv7) stored in the Android Room `outbox_messages` table before network transmission. If the connection drops before `msg.ack` arrives, the client retries with the same `client_dedup_key`. The server's `UNIQUE (client_dedup_key)` constraint guarantees idempotent persistence and returns the existing `serverMessageId` without creating duplicate messages.

---

## 5. Authentication, End-to-End Encryption & Privacy Architecture

### 5.1 Cryptographic Matrix & Honest Security Boundaries

| Conversation Type | Cryptographic Protocol | Key Management | Backend Visibility |
|---|---|---|---|
| **1:1 Private Chats** | **Extended Triple Diffie-Hellman (X3DH) + Double Ratchet** (Signal Protocol / `libsignal-client`) | Hardware-backed Identity Key in Android Keystore (`EC P-256` / `Curve25519`), signed pre-keys rotated weekly, one-time pre-keys. | **Zero plaintext access.** Server sees only routing metadata & ciphertext (`AES-256-GCM`). |
| **Private Encrypted Groups** | **Messaging Layer Security (MLS — RFC 9420)** | TreeKEM ratcheting group key tree; forward secrecy and post-compromise security on member join/leave. | **Zero plaintext access** to message or thread reply payloads. |
| **Public Communities & Broadcast Channels** | **TLS 1.3 in Transit + AES-256 At-Rest** (Explicitly badged as *"Public Community · Server-Indexed"*) | Server-managed channel keys so newly joining members can browse historical threads, resources, and public search. | Plaintext indexed for public discovery, moderation, and full-text search. |

### 5.2 Encrypted Attachments & Local Full-Text Search
1. **Attachment Encryption**: Before uploading any private image, video, PDF, or voice note, the Android client generates an ephemeral 256-bit symmetric key $K_{\text{file}}$ and 96-bit IV, encrypts the file stream using `AES/GCM/NoPadding`, computes `SHA-256(ciphertext)`, and uploads only the encrypted blob to Object Storage. $K_{\text{file}}$, the IV, and the SHA-256 hash are transmitted inside the E2EE message payload.
2. **Local Encrypted Search Index**: Because the server cannot search E2EE message bodies, the Android client maintains an on-device **SQLite FTS4** search index inside an encrypted database (`SQLCipher` / Android Keystore-wrapped key) so users can search across private messages, threads, and people instantaneously offline and online.
3. **Key Verification (Safety Numbers)**: Each 1:1 conversation and user profile exposes a 60-digit numeric Safety Fingerprint + scannable QR payload derived from `HKDF-SHA256(IdentityKey_A || IdentityKey_B)` so users can verify device authenticity out-of-band.

---

## 6. Security Threat Model (STRIDE)

| Threat & Vector | Impact | Architectural Mitigation |
|---|---|---|
| **Spoofing / Rogue Device Registration** | Attacker registers a shadow device on victim's account to intercept E2EE keys. | Firebase Auth token verification + existing devices receive an immediate security alert banner when a new `user_devices` entry is registered; Safety Number change warning displayed in chats. |
| **Tampering / Ciphertext Replay** | Attacker replays or alters an encrypted message or attachment blob. | `AES-GCM` 128-bit authentication tags reject modified ciphertexts; Double Ratchet message numbers & `client_dedup_key` reject replayed packets; attachment `sha256_ciphertext` verified before decryption. |
| **Information Disclosure / PII Scraping** | Bulk enumeration of phone numbers or unauthorized read of private profiles. | Split `users_public` vs `users_private` tables; phone lookup hashes rate-limited via Redis sliding-window token bucket (15 lookups/min); strict `privacy_phone_lookup` enforcement. |
| **Information Disclosure / Private Media Leak** | Guessing or sharing permanent CDN URLs for private attachments. | Object storage buckets are strictly private; download requires membership check and issues a 5-minute signed URL; payload itself is client-side AES-GCM encrypted. |
| **Denial of Service / WebSocket Flood** | Botnet floods group threads or opens thousands of idle sockets. | Per-UID and per-IP token-bucket rate limiting in Redis; max 64 KB WebSocket frame size; max 5 concurrent device sockets per user. |
| **Elevation of Privilege / Group Admin Bypass** | Regular member deletes threads, pins announcements, or posts in broadcast channel. | Server-side RBAC check against `conversation_members.role` (`OWNER`, `ADMIN`, `MODERATOR`) on every state-mutating endpoint and security rule. |

---

## 7. Infrastructure Deployment, Analytics & Cost Scaling

### 7.1 Progressive Infrastructure Plan
- **Stage 1 — Launch & Seed (0 to 50,000 MAU — ~$65–$140/month)**:
  - **Compute**: 2× stateless Modular Monolith containers (2 vCPU, 2 GB RAM) behind Cloud Run / Fly.io / Render auto-scaling with sticky WebSocket routing.
  - **Database**: Managed PostgreSQL (Neon / Supabase / Cloud SQL — 2 vCPU, 4 GB RAM, PgBouncer connection pooling, daily automated snapshots + point-in-time recovery).
  - **Cache & Pub/Sub**: Managed Redis (Upstash / Memorystore — 1 GB) for WebSocket session routing, online presence, and rate limiting.
  - **Object Storage**: Cloudflare R2 (zero egress fees for media downloads) with lifecycle policies expiring unreferenced staging uploads after 24 hours.
  - **Calling**: Managed LiveKit Cloud or Cloudflare Calls SFU (pay-per-minute usage tier).
- **Stage 2 — Growth (50,000 to 500,000 MAU)**:
  - Add PostgreSQL read replica for community discovery and channel history queries; extract the **WebSocket Real-Time Gateway** module into a dedicated horizontally scaled deployment fed by Redis Pub/Sub.

### 7.2 Privacy-Preserving Product Analytics, Moderation & Admin
- **Zero-Plaintext Analytics**: Track aggregate operational metrics (DAU/MAU, thread reply ratios, call connection latency, crash-free sessions) without ever logging message text, search queries, or contact graphs.
- **Moderation & Compliance**: Users can block accounts, report messages/threads/groups, export their account metadata (GDPR/CCPA JSON archive), and permanently delete their account with cascading cryptographic key erasure.

---

## 8. Testing Strategy

1. **Security & Access Control Rules Testing**:
   - **Tier 1 (Fast Rule Validation)**: Automated unit tests verifying unauthenticated rejection, cross-user PII isolation, schema validation bounds, and RBAC role enforcement.
   - **Tier 2 (Robolectric JVM Integration)**: End-to-end repository tests verifying conversation creation, parent message posting, thread reply counter updates, subscription toggling, and unauthorized access denial.
2. **Cryptographic & Protocol Unit Tests**:
   - Deterministic test vectors for ECDH key agreement, `AES/GCM/NoPadding` encryption/decryption round-trips, IV uniqueness, tampered ciphertext rejection, and Safety Fingerprint formatting.
3. **Critical User Journey (CUJ) UI & State Verification**:
   - Automated verification of onboarding $\rightarrow$ Auth & OTP setup $\rightarrow$ Group topic switching $\rightarrow$ Opening a thread $\rightarrow$ Sending a thread reply $\rightarrow$ Verifying parent message reply badge increment.

---

## 9. Prioritized MVP Feature List & Revised 6-Phase Milestones

### Prioritized MVP Scope (Implemented in Full Working Code)
1. **Liquid Glass Design System & Adaptive Navigation**: Three-tier glass surfaces, ambient background lighting, custom `Inter` typography, animated splash, 3-step onboarding, and floating capsule navigation.
2. **Authentication, Phone OTP & Profile Identity**: Google Sign-In via Credential Manager + phone number/country code & 6-digit OTP verification flow + mandatory display name and unique `@username` registration.
3. **Private E2EE Messaging**: Real-time 1:1 chats with hardware-backed Android Keystore ECDH + AES-GCM encryption, typing indicators, delivery/read receipts, emoji reactions, inline edit/delete, voice notes, and attachments.
4. **Signature Threaded Group Conversations**: Group header with member/online counts, horizontal Topic bar (*All, General, Announcements, Resources, Questions*), compact group messages with live thread badges (`"12 replies · View thread"`), slide-in / split-pane **Thread Panel**, thread follow/mute subscriptions, `@mentions`, within-thread search, jump-to-parent, and group resource library.
5. **Communities & Broadcast Channels**: Public/private community discovery, category filtering, invite code joining, and broadcast channels with pinned announcements and interactive live polls.
6. **Voice & Video Calling**: Call history tab (*All, Incoming, Outgoing, Missed*) and live voice/video call interface with CameraX self-preview, audio waveform visualizer, mute/speaker/camera flip controls, and real-time call signaling.
7. **Security, Moderation & Settings**: Key verification fingerprint viewer, active device management, key rotation, user blocking, abuse reporting (*User, Message, Group*), and full Account/Privacy/Notification/Appearance (Reduced Motion) settings.

### Incremental Implementation Milestones
- **Milestone 1 — Foundation**: Project configuration, `Inter` typography, custom launcher icon, Liquid Glass design system, authentication + OTP/handle setup, user profile management, and global search.
- **Milestone 2 — Private & Group Messaging**: Real-time 1:1 and group conversations, message composer, attachments, voice notes, contextual actions (pin, mute, archive, mark unread, delete), and offline-resilient state.
- **Milestone 3 — Signature Threaded Experience**: Group topics, parent-to-thread slide-over/split panel, thread replies, follow/mute subscriptions, `@mentions`, reactions, read receipts drawer, polls, and group resource library.
- **Milestone 4 — Security & E2EE Engine**: Android Keystore ECDH + AES-GCM encryption engine, attachment encryption metadata, Safety Fingerprint verification UI, and device session management.
- **Milestone 5 — Communities, Channels & Calling**: Community discovery & invites, broadcast channels, moderation/reporting workflows, call history, and live CameraX/audio voice & video call screen.
- **Milestone 6 — Verification & Launch Polish**: Firestore security rules validation, Robolectric repository tests, accessibility & reduced-motion support, and final compilation verification.
