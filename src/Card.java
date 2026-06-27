/**
 * Represents a card with attack, health, and revival properties.
 */
public class Card {

    /** Card identity */
    String name;

    /** Base and current attack/health values */
    int Ainit, Hinit;
    int Abase, Hbase;
    int Acur, Hcur;

    /** Type-2 variables */
    int revivalProgress;
    int Hmissing;

    /** Tie-breakers */
    long orderId;
    long discardId;

    /** State flags */
    boolean inDeck;
    boolean inDiscard;

    /**
     * Creates a new card with given stats.
     */
    public Card(String name, int a, int h, long ord) {
        this.name = name;
        this.Ainit = a; this.Hinit = h;
        this.Abase = a; this.Hbase = h;
        this.Acur = a;  this.Hcur = h;
        this.revivalProgress = 0;
        this.Hmissing = 0;
        this.orderId = ord;
        this.discardId = -1;
        this.inDeck = true;
        this.inDiscard = false;
    }

    /**
     * Updates current attack proportional to current health.
     */
    public void recomputeAcurFromHealth() {
        long num = 1L * Abase * Math.max(0, Hcur);
        int val = (int)(num / Math.max(1, Hbase));
        if (val < 1) val = 1;
        this.Acur = val;
    }

    /**
     * Sends the card to discard after death.
     */
    public void moveToDiscard(long newDiscardId) {
        inDeck = false;
        inDiscard = true;
        this.Hcur = 0;
        this.revivalProgress = 0;
        this.Hmissing = this.Hbase;
        this.discardId = newDiscardId;
    }

    /**
     * Fully revives the card with 10% attack loss.
     */
    public void fullRevive(long newOrderId) {
        this.Abase = (int)Math.max(1, Math.floor(this.Abase * 0.90));
        this.Hcur = this.Hbase;
        this.revivalProgress = this.Hbase;
        this.Hmissing = 0;
        this.Acur = this.Abase;
        inDiscard = false;
        inDeck = true;
        this.orderId = newOrderId; /** re-entry to deck = new order */
    }

    /**
     * Partially revives the card with 5% attack loss.
     */
    public void partialReviveSpendAll(int spend, long newId) {
        this.revivalProgress += spend;
        this.Hmissing = this.Hbase - this.revivalProgress;
        this.Abase = (int)Math.max(1, Math.floor(this.Abase * 0.95));
        this.discardId =newId;
    }
}
