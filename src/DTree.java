/**
 * Discard-pile AVL tree keyed by (Hmissing, discardId).
 * - Used in Type-2 healing to pick full/partial revives fast.
 */
public class DTree {

    /** AVL node storing one discarded card. */
    static class DNode {
        int Hmissing;  // remaining HP needed to fully revive
        long did; // discard order id
        Card card;
        DNode left,right;
        int height;
        int subMinHmiss, subMaxHmiss;
        DNode(Card c){
            this.Hmissing=c.Hmissing;
            this.did=c.discardId; this.card=c;
            this.height=1;
            this.subMinHmiss=this.Hmissing; this.subMaxHmiss=this.Hmissing;
        }
    }

    DNode root;

    // ---- Small AVL helpers ----
    static int h(DNode n){ return n==null?0:n.height; }
    static int minH(DNode n){ return n==null?Integer.MAX_VALUE:n.subMinHmiss; }
    static int maxH(DNode n){ return n==null?Integer.MIN_VALUE:n.subMaxHmiss; }

    /** Recompute height and subtree aggregates after a structural change. */
    static void pull(DNode n){
        if(n==null) return;
        n.height = 1 + Math.max(h(n.left), h(n.right));
        n.subMinHmiss = Math.min(n.Hmissing, Math.min(minH(n.left), minH(n.right)));
        n.subMaxHmiss = Math.max(n.Hmissing, Math.max(maxH(n.left), maxH(n.right)));
    }

    /** Standard AVL balance factor. */
    static int bal(DNode n){ return (n==null)?0:h(n.left)-h(n.right); }

    /** Total order: first by Hmissing, then by discardId. */
    static int cmp(int h1,long d1, int h2,long d2){
        if(h1 != h2) return h1 < h2 ? -1 : 1;
        if(d1 != d2) return d1 < d2 ? -1 : 1;
        return 0;
    }

    // ---- Rotations ----
    static DNode rotR(DNode y){ DNode x=y.left; DNode T=x.right;
        x.right=y; y.left=T;
        pull(y); pull(x);
        return x; }

    static DNode rotL(DNode x){ DNode y=x.right; DNode T=y.left;
        y.left=x; x.right=T;
        pull(x); pull(y);
        return y; }

    /** Insert card by (Hmissing, discardId) keeping AVL balance. */
    DNode insert(DNode n, Card c){
        if(n==null) return new DNode(c);
        int comp = cmp(c.Hmissing, c.discardId, n.Hmissing, n.did);
        if(comp<0) n.left = insert(n.left,c);
        else if(comp>0) n.right = insert(n.right,c);
        pull(n);
        int b=bal(n);
        if(b>1){
            // Left heavy
            if(cmp(c.Hmissing,c.discardId,n.left.Hmissing,n.left.did)<0)
                return rotR(n);
            else { n.left=rotL(n.left); return rotR(n); }
        }
        if(b<-1){
            // Right heavy
            if(cmp(c.Hmissing,c.discardId,n.right.Hmissing, n.right.did)>0)
                return rotL(n);
            else { n.right=rotR(n.right); return rotL(n); }
        }
        return n;
    }

    /** Smallest node in subtree (by key). */
    DNode minNode(DNode n){ while(n.left!=null) n=n.left; return n; }

    /** Remove specific card (by its current keys) keeping AVL balance. */
    DNode delete(DNode n, Card c){
        if(n==null) return null;
        int comp = cmp(c.Hmissing, c.discardId, n.Hmissing, n.did);
        if(comp<0) n.left = delete(n.left, c);
        else if(comp>0) n.right = delete(n.right, c);
        else{
            // Found target
            if(n.left==null || n.right==null) n = (n.left!=null)?n.left:n.right;
            else{
                // Replace with inorder successor
                DNode m = minNode(n.right);
                n.Hmissing=m.Hmissing;
                n.did=m.did; n.card=m.card;
                n.right = delete(n.right,m.card);
            }
        }
        if(n==null) return null;
        pull(n);
        int b=bal(n);
        if(b>1){
            if(bal(n.left)>=0) return rotR(n);
            else { n.left=rotL(n.left); return rotR(n); }
        }
        if(b<-1){
            if(bal(n.right)<=0) return rotL(n);
            else { n.right=rotR(n.right); return rotL(n); }
        }
        return n;
    }

    /** Insert card to discard AVL. */
    public void insert(Card c){ root = insert(root,c); }

    /** Remove card from discard AVL. */
    public void remove(Card c){ root = delete(root,c); }

    /** True if discard pile is empty. */
    public boolean isEmpty(){ return root==null; }

    /**
     * Pick the card with the largest Hmissing that is ≤ pool (ties: earliest discardId).
     * Used for "full revive" selection.
     */
    public Card largestHmissLE(int pool){
        DNode n=root; Card ans=null; int bestH=-1; long bestDid=Long.MAX_VALUE;
        while(n!=null){
            if(n.Hmissing <= pool){
                if(n.Hmissing > bestH || (n.Hmissing == bestH && ( n.did < bestDid))){
                    ans = n.card; bestH = n.Hmissing;  bestDid = n.did;
                }
                n = n.right;
            }else{
                n = n.left;
            }
        }
        return ans;
    }

    /**
     * Pick the smallest Hmissing in the entire tree (ties: earliest discardId due to inorder).
     * Used for the single "partial revive" target when no full revive fits.
     */
    public Card smallestForPartial(){
        DNode n=root; if(n==null) return null;
        while(n.left!=null) n=n.left;
        return n.card;
    }
}
