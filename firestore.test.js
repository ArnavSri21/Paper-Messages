const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_1";
const BOB_UID = "bob_user_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
});

test("Authenticated user can create and read their own profile and conversation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Vance",
      username: "alice_v",
      phoneNumber: "+15550192831",
      countryCode: "+1",
      bio: "Security researcher & product builder",
      avatarColor: "#8B7CFF",
      publicKeyBase64: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE",
      keyFingerprint: "8491 2049 5518 9920",
      deviceName: "Pixel 9 Pro",
      onboardingCompleted: true,
      online: true,
      privacyOnline: "EVERYONE",
      privacyLastSeen: "EVERYONE",
      privacyReadReceipts: true,
      privacyPhoneLookup: "CONTACTS",
      reducedMotion: false,
      blockedUsersCsv: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1").set({
      userId: ALICE_UID,
      title: "Core Architecture Group",
      subtitle: "42 members · 11 online",
      type: "GROUP",
      encryptionMode: "E2EE_MLS",
      memberCount: 42,
      onlineCount: 11,
      lastMessagePreview: "How should we approach the upcoming project?",
      lastMessageSender: "Elena Rostova",
      unreadCount: 2,
      pinned: true,
      muted: false,
      archived: false,
      topicsCsv: "General,Announcements,Resources,Questions",
      resourcesCsv: "Architecture_Spec_v2.pdf|Design_Tokens.fig",
      inviteCode: "PAPER-ARCH-26",
      avatarColor: "#8B7CFF",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's conversations or threads", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_secret")
      .set({
        userId: ALICE_UID,
        title: "Secret Strategy",
        subtitle: "Encrypted",
        type: "PRIVATE",
        encryptionMode: "E2EE_SIGNAL",
        memberCount: 2,
        onlineCount: 2,
        lastMessagePreview: "Encrypted payload",
        lastMessageSender: "Alice",
        unreadCount: 0,
        pinned: false,
        muted: false,
        archived: false,
        topicsCsv: "General",
        resourcesCsv: "",
        inviteCode: "PRIV-1",
        avatarColor: "#54D6E8",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_secret").get()
  );
});

test("Shadow field injection is rejected by strict schema rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Vance",
      username: "alice_v",
      phoneNumber: "+15550192831",
      countryCode: "+1",
      bio: "Bio",
      avatarColor: "#8B7CFF",
      publicKeyBase64: "KEY",
      keyFingerprint: "FP",
      deviceName: "Device",
      onboardingCompleted: true,
      online: true,
      privacyOnline: "EVERYONE",
      privacyLastSeen: "EVERYONE",
      privacyReadReceipts: true,
      privacyPhoneLookup: "CONTACTS",
      reducedMotion: false,
      blockedUsersCsv: "",
      isAdminGhostField: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
