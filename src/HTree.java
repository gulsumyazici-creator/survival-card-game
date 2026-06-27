/**
 * AVL tree that indexes cards in a bucket by (Hcur, orderId).
 * Used inside ATree nodes to tie-break on health, then insertion order.
 */
public class HTree {

    /** AVL node keyed by (H, ord) and carrying a Card reference. */
    static class HNode {
        int H;  // key: current health
        long ord; // key: orderId
        Card card;
        HNode left, right; // children
        int height; // AVL height
        int minH, maxH; // subtree H range for pruning

        HNode(Card c){
            this.H=c.Hcur;
            this.ord=c.orderId;
            this.card=c;
            this.height=1;
            this.minH=this.H;
            this.maxH=this.H;
        }
    }

    HNode root;

    /** @return node height or 0 if null */
    static int h(HNode n) {
        if (n == null) {
            return 0;
        } else {
            return n.height;
        }
    }

    /** @return subtree minimum H (∞ if null) */
    static int minH(HNode n) {
        if (n == null) {
            return Integer.MAX_VALUE;
        } else {
            return n.minH;
        }
    }

    /** @return subtree maximum H (-∞ if null) */
    static int maxH(HNode n) {
        if (n == null) {
            return Integer.MIN_VALUE;
        } else {
            return n.maxH;
        }
    }

    /** Recompute height and (minH,maxH) aggregates. */
    static void pull(HNode n){
        if(n==null) return;
        n.height = 1 + Math.max(h(n.left), h(n.right));
        n.minH = Math.min(n.H, Math.min(minH(n.left), minH(n.right)));
        n.maxH = Math.max(n.H, Math.max(maxH(n.left), maxH(n.right)));
    }

    /** Total order by (H, ord). */
    static int cmp(int H1,long o1,int H2,long o2){
        if(H1!=H2) return H1<H2?-1:1;
        if(o1!=o2) return o1<o2?-1:1;
        return 0;
    }

    /** Right rotation. */
    static HNode rotR(HNode y){
        HNode x=y.left; HNode T=x.right;
        x.right=y; y.left=T;
        pull(y); pull(x);
        return x; }

    /** Left rotation. */
    static HNode rotL(HNode x){
        HNode y=x.right; HNode T=y.left;
        y.left=x; x.right=T;
        pull(x); pull(y);
        return y; }

    /** Balance factor (LH - RH). */
    static int bal(HNode n){
        return (n==null)?0:h(n.left)-h(n.right);
    }

    /** Insert by (c.Hcur, c.orderId) and AVL-rebalance. */
    HNode insert(HNode n, Card c){
        if(n==null) return new HNode(c);

        int comp = cmp(c.Hcur, c.orderId, n.H, n.ord);

        if(comp<0) n.left = insert(n.left, c);
        else if(comp>0) n.right = insert(n.right, c);

        pull(n);
        int b=bal(n);

        if(b>1){
            if(cmp(c.Hcur,c.orderId,n.left.H,n.left.ord)<0) return rotR(n);
            else { n.left=rotL(n.left); return rotR(n); }
        }
        if(b<-1){
            if(cmp(c.Hcur,c.orderId,n.right.H,n.right.ord)>0) return rotL(n);
            else { n.right=rotR(n.right); return rotL(n); }
        }
        return n;
    }

    /** Leftmost (minimum) node. */
    HNode minNode(HNode n){
        while(n.left!=null) n=n.left;
        return n;
    }

    /** Delete by (H, ord) and AVL-rebalance. */
    HNode delete(HNode n, int H, long ord){
        if(n==null) return null;

        int comp = cmp(H,ord,n.H,n.ord);

        if(comp<0) n.left = delete(n.left,H,ord);
        else if(comp>0) n.right = delete(n.right,H,ord);
        else{
            if (n.left == null || n.right == null) {
                if (n.left != null)
                    n = n.left;
                else
                    n = n.right;
            }else{
                HNode m = minNode(n.right);
                n.H=m.H; n.ord=m.ord; n.card=m.card;
                n.right = delete(n.right,m.H,m.ord);
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

    /** Public insert/remove wrappers. */
    public void insert(Card c){ root = insert(root,c); }
    public void remove(Card c){ root = delete(root,c.Hcur,c.orderId); }

    /**Quickly checks if the subtree contains any card with health (H) above or below a given threshold. */
    public static boolean hasHGreaterThan(HNode r, int thr){ return maxH(r) > thr; }
    public static boolean hasHLessOrEq(HNode r, int thr){ return minH(r) <= thr; }

    /**
     * Returns card with smallest (H,ord) such that H > thr, or null if none.
     * Used to pick minimal-damage survivor in a bucket.
     */
    public Card minHGreaterThan(int thr){
        HNode n=root; Card ans=null; int bestH=Integer.MAX_VALUE; long bestO=Long.MAX_VALUE;
        while(n!=null){
            if(maxH(n.left) > thr){ n = n.left; continue; }
            if(n.H > thr){
                if(n.H < bestH || (n.H==bestH && n.ord<bestO)){ ans=n.card; bestH=n.H; bestO=n.ord; }
                n = n.left; continue;
            }
            n = n.right;
        }
        return ans;
    }

    /**
     * Returns card with minimal H among those with H <= thr (tie: earliest ord).
     * Used when no survivor exists and we pick a dying card by min-H rule.
     */
    public Card minHLessOrEq(int thr){
        HNode n=root; Card ans=null; int bestH=Integer.MAX_VALUE; long bestO=Long.MAX_VALUE;
        while(n!=null){
            if(n.H <= thr){
                if(n.H < bestH || (n.H==bestH && n.ord<bestO)){ ans=n.card; bestH=n.H; bestO=n.ord; }
                n = n.left;
            }else{
                n = n.left;
            }
        }
        return ans;
    }
}
