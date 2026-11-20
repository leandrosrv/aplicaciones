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

test("Unauthenticated user cannot read or write workoutSets or muscleGroups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workoutSets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("muscleGroups").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workoutSets").doc("set_1").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80.5,
      reps: 8,
      rir: 2,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, list, partial update, and delete workoutSets matching Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("workoutSets");

  // Create without optional fields (omitted via .filterValues { it != null })
  await assertSucceeds(
    col.doc("set_1").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create with optional pesoCorporal and obs
  await assertSucceeds(
    col.doc("set_2").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press inclinado",
      serie: 2,
      peso: 62.5,
      reps: 10,
      rir: 1,
      pesoCorporal: 74.2,
      obs: "Buena técnica",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(col.get());

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    col.doc("set_1").update({
      peso: 82.5,
      reps: 9,
      rir: 1,
      obs: "Subiendo carga",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete
  await assertSucceeds(col.doc("set_2").delete());
});

test("Reject invalid workoutSets payloads, @DocumentId id injection, future timestamps, and bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("workoutSets");

  // Extra "id" field (forbidden when @DocumentId is used)
  await assertFails(
    col.doc("bad_id_field").set({
      id: "bad_id_field",
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Ghost field injection
  await assertFails(
    col.doc("bad_ghost").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 2,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Invalid date format
  await assertFails(
    col.doc("bad_date").set({
      userId: ALICE_UID,
      fecha: "20-11-2026",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Out-of-bounds RIR (> 5)
  await assertFails(
    col.doc("bad_rir").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 6,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp
  await assertFails(
    col.doc("bad_future").set({
      userId: ALICE_UID,
      fecha: "2026-11-20",
      grupo: "Pecho",
      ejercicio: "Press banca",
      serie: 1,
      peso: 80,
      reps: 8,
      rir: 2,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's workoutSets or muscleGroups", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const bobDb = ctx.firestore();
    const now = new Date(Date.now() - 10000);
    await bobDb.collection("users").doc(BOB_UID).collection("workoutSets").doc("bob_set").set({
      userId: BOB_UID,
      fecha: "2026-11-20",
      grupo: "Espalda",
      ejercicio: "Dominadas",
      serie: 1,
      peso: 10,
      reps: 10,
      rir: 2,
      createdAt: now,
      updatedAt: now,
    });
    await bobDb.collection("users").doc(BOB_UID).collection("muscleGroups").doc("bob_grp").set({
      userId: BOB_UID,
      name: "Antebrazos",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("workoutSets").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("workoutSets").doc("bob_set").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("workoutSets").doc("bob_set").update({ peso: 99 }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("workoutSets").doc("bob_set").delete());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("muscleGroups").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("muscleGroups").doc("bob_grp").delete());
});

test("Owner can create, list, and delete custom muscleGroups; rejects invalid names", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const grpCol = aliceDb.collection("users").doc(ALICE_UID).collection("muscleGroups");

  await assertSucceeds(
    grpCol.doc("grp_1").set({
      userId: ALICE_UID,
      name: "Antebrazos",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(grpCol.get());
  await assertSucceeds(grpCol.doc("grp_1").delete());

  // Empty name fails
  await assertFails(
    grpCol.doc("grp_bad").set({
      userId: ALICE_UID,
      name: "",
      createdAt: created,
      updatedAt: created,
    })
  );
});
