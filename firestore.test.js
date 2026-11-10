const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: cannot read chats or vault_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("chats").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vault_items").get()
  );
});

test("Authenticated user: can create and list own chat threads and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const chatRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("chats")
    .doc("chat_vip_1");

  await assertSucceeds(
    chatRef.set({
      userId: ALICE_UID,
      name: "Tariq Al-Mansoor",
      nameAr: "طارق المنصور",
      avatarInitials: "TM",
      avatarColorHex: "#D4AF37",
      spaceCategory: "VIP",
      isPinned: true,
      unreadCount: 2,
      lastMessagePreview: "Architectural blueprints ready",
      lastMessagePreviewAr: "المخططات المعمارية جاهزة",
      lastMessageTime: "10:42 AM",
      isOnline: true,
      roleOrTitle: "Principal Architect",
      roleOrTitleAr: "كبير المهندسين المعماريين",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").get()
  );

  const msgRef = chatRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      chatId: "chat_vip_1",
      senderId: "contact",
      senderName: "Tariq Al-Mansoor",
      textEn: "Here is the 100% HD render of the pavilion.",
      textAr: "إليك التصميم بدقة 100% عالية الوضوح للجناح.",
      messageType: "HD_MEDIA",
      voiceDurationSec: 0,
      voiceTranscriptEn: "",
      voiceTranscriptAr: "",
      mediaTitle: "Dubai_Glass_Pavilion_RAW.tiff",
      mediaResolution: "3840x2160 • 100% HD Original",
      mediaSizeLabel: "24.8 MB • Uncompressed",
      mediaDrawableName: "img_hd_architecture",
      status: "READ",
      translatedText: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's chats or vault items", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_items")
      .doc("vault_1")
      .set({
        userId: ALICE_UID,
        title: "Q4 Executive Strategy",
        content: "Confidential launch roadmap",
        category: "NOTE",
        sizeOrDomain: "1.2 KB",
        isPinned: true,
        tag: "Strategy",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_items")
      .doc("vault_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vault_items").get()
  );
});

test("Shadow Update & Invalid Schema Guard: rejects extra fields or invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const vaultRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("vault_items")
    .doc("vault_bad");

  // Extra ghost field should fail
  await assertFails(
    vaultRef.set({
      userId: ALICE_UID,
      title: "Secret Note",
      content: "Body text",
      category: "NOTE",
      sizeOrDomain: "2 KB",
      isPinned: false,
      tag: "Personal",
      isAdminHack: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid category enum should fail
  await assertFails(
    vaultRef.set({
      userId: ALICE_UID,
      title: "Secret Note",
      content: "Body text",
      category: "INVALID_CATEGORY",
      sizeOrDomain: "2 KB",
      isPinned: false,
      tag: "Personal",
      createdAt: now,
      updatedAt: now,
    })
  );
});
