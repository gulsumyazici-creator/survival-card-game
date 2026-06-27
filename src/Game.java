/**
 * Main game logic for the Survivor vs Stranger card duel.
 * Manages deck, discard, scoring, and Type-2 healing phases.
 */
public class Game {

    // Global counters for tiebreakers
    private long globalOrderId = 0;
    private long globalDiscardId = 0;

    // Scores
    private long survivorScore = 0;
    private long strangerScore  = 0;

    // Structures
    private ATree deckTree = new ATree();
    private DTree discardTree = new DTree();
    private int deckCount = 0;
    private int discardCount = 0;

    // Mode
    private boolean type2;// Type-2 healing enabled?

    public Game(boolean type2){
        this.type2 = type2;
    }

    // ---------- Deck & discard helpers ----------
    private void deckInsert(Card c){
        deckTree.insert(c);
        deckCount++;
        c.inDeck=true;
    }
    private void deckRemove(Card c){
        deckTree.remove(c);
        deckCount--;
        c.inDeck=false;
    }
    private void discardInsert(Card c){
        discardTree.insert(c);
        discardCount++;
        c.inDiscard=true;
    }
    private void discardRemove(Card c){
        discardTree.remove(c);
        discardCount--;
        c.inDiscard=false;
    }

    // ===== Commands =====

    /** Adds a new card to the deck. */
    public String cmd_draw_card(String name, int att, int hp){
        Card c = new Card(name, att, hp, ++globalOrderId);
        deckInsert(c);
        return "Added " + name + " to the deck\n";
    }

    /** Stores a single battle’s result and revival count. */
    static class BattleResult {
        String outLine;
        int revivedCount;
    }

    /**
     * Executes one battle:
     *  - Finds a playable card by priority (P1→P2→P3→P4)
     *  - Applies damage and score changes
     *  - Runs Type-2 healing (if enabled)
     */
    public BattleResult cmd_battle(int Astr, int Hstr, int healPool){
        BattleResult br = new BattleResult();
        br.revivedCount = 0;

        // --- Priority search (P1→P4) ---
        Card chosen = null; int priority = -1;
        Card p1 = deckTree.findPriority1(Hstr, Astr);
        if (p1 != null) { chosen = p1; priority = 1; }
        else {
            Card p2 = deckTree.findPriority2(Hstr, Astr);
            if (p2 != null) { chosen = p2; priority = 2; }
            else {
                Card p3 = deckTree.findPriority3(Hstr, Astr);
                if (p3 != null) { chosen = p3; priority = 3; }
                else {
                    Card p4 = deckTree.findPriority4();
                    if (p4 != null) { chosen = p4; priority = 4; }
                }
            }
        }

        int revivedThisTurn = 0;

        // No playable card
        if (chosen == null) {
            strangerScore += 2; // automatic +2 to Stranger
            revivedThisTurn += healingPhase(healPool);
            br.revivedCount = revivedThisTurn;
            br.outLine = "No cards to play, " + revivedThisTurn + " cards revived\n";
            return br;
        }

        // ---- Apply results ----
        deckRemove(chosen); // remove before modifying

        // ---- Simultaneous damage ----
        int Hcur_prime = chosen.Hcur - Astr;
        int Hstr_prime = Hstr - chosen.Acur;

        // Clamp health values to [0, Hbase]
        if (Hcur_prime < 0) Hcur_prime = 0;
        if (Hstr_prime < 0) Hstr_prime = 0;

        boolean survivorAlive = Hcur_prime > 0;
        boolean strangerAlive = Hstr_prime > 0;

        // +2 for kills
        if (!survivorAlive) strangerScore += 2;
        if (!strangerAlive) survivorScore += 2;

        // +1 for surviving (no “took damage” check)
        if (survivorAlive && Hcur_prime <= chosen.Hbase) {
            strangerScore += 1;
        }
        if (strangerAlive && Hstr_prime <= Hstr) {
            survivorScore += 1;
        }

        // --- Apply card outcomes ---
        if (Hcur_prime == 0) { // dead
            chosen.Hcur = 0;
            chosen.moveToDiscard(++globalDiscardId);
            discardInsert(chosen);
            revivedThisTurn += healingPhase(healPool);
            br.revivedCount = revivedThisTurn;
            br.outLine = "Found with priority " + priority + ", Survivor plays " + chosen.name +
                    ", the played card is discarded, " + revivedThisTurn + " cards revived\n";
        } else { // survived
            chosen.Hcur = Hcur_prime;
            chosen.recomputeAcurFromHealth(); // Abase * Hcur/Hbase → clamp(≥1)
            chosen.orderId = ++globalOrderId;
            deckInsert(chosen);
            revivedThisTurn += healingPhase(healPool);
            br.revivedCount = revivedThisTurn;
            br.outLine = "Found with priority " + priority + ", Survivor plays " + chosen.name +
                    ", the played card returned to deck, " + revivedThisTurn + " cards revived\n";
        }

        return br;
    }

    /**
     * Healing phase for Type-2 mode.
     * 1. Fully revive as many cards as pool allows (largest Hmissing ≤ pool)
     * 2. If pool remains, partially revive the smallest Hmissing card
     */
    private int healingPhase(int healPool){
        if (!type2 || healPool <= 0) return 0;

        int revivedCountThisTurn = 0;

        // --- Full revives ---
        while (true) {
            Card full = discardTree.largestHmissLE(healPool);

            if (full == null) break;

            int cost = full.Hmissing;

            discardRemove(full);
            full.fullRevive(++globalOrderId);
            deckInsert(full);

            healPool -= cost;
            revivedCountThisTurn++;

            if (healPool <= 0) break;
        }

        // --- Partial revive if pool remains ---
        if (healPool > 0 && !discardTree.isEmpty()) {
            Card partial = discardTree.smallestForPartial();
            if (partial != null && partial.Hmissing > 0) {

                discardRemove(partial);

                partial.partialReviveSpendAll(
                        healPool, ++globalDiscardId
                );

                discardInsert(partial); // stays in discard
            }
        }

        return revivedCountThisTurn;
    }

    /** Returns total number of cards in deck. */
    public String cmd_deck_count(){
        return "Number of cards in the deck: " + deckCount + "\n";
    }

    /** Returns total number of cards in discard pile (only in Type-2). */
    public String cmd_discard_count(){
        if(!type2) return "Number of cards in the discard pile: 0\n";
        return "Number of cards in the discard pile: " + discardCount + "\n";
    }

    /** Prints which side is winning by score. */
    public String cmd_find_winning(){
        long sSurvivor = survivorScore;
        long sStranger = strangerScore;

        return (sSurvivor >= sStranger)
                ? "The Survivor, Score: " + sSurvivor + "\n"
                : "The Stranger, Score: " + sStranger + "\n";
    }

    /**
     * Stranger steals the weakest eligible card (A > aLim and H > hLim).
     * Removes it permanently from both deck and discard.
     */
    public String cmd_steal_card(int aLim, int hLim){
        Card target = deckTree.stealFirstGreaterAWithH(aLim, hLim);
        if(target==null) return "No card to steal\n";
        deckRemove(target);
        target.inDeck=false;
        target.inDiscard=false;
        return "The Stranger stole the card: " + target.name + "\n";
    }
}
