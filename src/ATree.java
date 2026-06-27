/**
 * Balanced AVL tree indexed by Attack (Acur) values.
 * Each node stores a bucket (HTree) for cards with the same attack.
 * Used for fast selection and removal of cards based on game priorities.
 */
public class ATree {

    /** Node representing one distinct attack value, storing all cards with that attack. */
    static class ANode {
        int A;
        ANode left,right;
        int height;

        // Subtree bounds for attack (A) and health (H)
        int subMinA, subMaxA;
        int subMinH, subMaxH;

        // Bucket of cards sharing same attack
        HTree bucket;

        ANode(int A, Card c){
            this.A=A; this.height=1;
            this.bucket = new HTree();
            this.bucket.insert(c);
            this.subMinA=A; this.subMaxA=A;
            this.subMinH=c.Hcur; this.subMaxH=c.Hcur;
        }
    }

    ANode root;

    // ---------- Helper Functions ----------
    static int h(ANode n){ return n==null?0:n.height; }
    static int minA(ANode n){ return n==null?Integer.MAX_VALUE:n.subMinA; }
    static int maxA(ANode n){ return n==null?Integer.MIN_VALUE:n.subMaxA; }
    static int minH(ANode n){ return n==null?Integer.MAX_VALUE:n.subMinH; }
    static int maxH(ANode n){ return n==null?Integer.MIN_VALUE:n.subMaxH; }

    /** Recomputes height and subtree min/max values after rotations or updates. */
    static void pull(ANode n){
        if(n==null) return;
        n.height = 1 + Math.max(h(n.left), h(n.right));
        n.subMinA = Math.min(n.A, Math.min(minA(n.left), minA(n.right)));
        n.subMaxA = Math.max(n.A, Math.max(maxA(n.left), maxA(n.right)));

        int selfMinH = HTree.minH(n.bucket.root);
        int selfMaxH = HTree.maxH(n.bucket.root);
        int leftMinH = minH(n.left), rightMinH = minH(n.right);
        int leftMaxH = maxH(n.left), rightMaxH = maxH(n.right);
        n.subMinH = Math.min(selfMinH, Math.min(leftMinH, rightMinH));
        n.subMaxH = Math.max(selfMaxH, Math.max(leftMaxH, rightMaxH));
    }
    static int bal(ANode n){ return (n==null)?0:h(n.left)-h(n.right); }

    static ANode rotR(ANode y){
        ANode x=y.left; ANode T=x.right;
        x.right=y; y.left=T;
        pull(y); pull(x);
        return x; }

    static ANode rotL(ANode x){
        ANode y=x.right; ANode T=y.left;
        y.left=x; x.right=T;
        pull(x); pull(y);
        return y; }

    /** Inserts a card by its attack value into the AVL tree. */
    ANode insert(ANode n, int A, Card c){
        if(n==null) return new ANode(A,c);
        if(A < n.A) n.left = insert(n.left,A,c);
        else if(A > n.A) n.right = insert(n.right,A,c);
        else n.bucket.insert(c);
        pull(n);
        int b=bal(n);
        if(b>1){
            if(A < n.left.A) return rotR(n);
            else { n.left=rotL(n.left); return rotR(n); }
        }
        if(b<-1){
            if(A > n.right.A) return rotL(n);
            else { n.right=rotR(n.right); return rotL(n); }
        }
        return n;
    }

    ANode minNode(ANode n){
        while(n.left!=null) n=n.left;
        return n; }

    /** Deletes an entire attack node if its bucket becomes empty. */
    ANode deleteWholeANode(ANode n, int A){
        if(n==null) return null;
        if(A < n.A) n.left = deleteWholeANode(n.left,A);
        else if(A > n.A) n.right = deleteWholeANode(n.right,A);
        else{
            if(n.left==null || n.right==null) n = (n.left!=null)?n.left:n.right;
            else {
                ANode m = minNode(n.right);
                n.A = m.A;
                n.bucket = m.bucket;
                n.right = deleteWholeANode(n.right, m.A);
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

    /** Deletes a specific card from its attack bucket and removes empty nodes if needed. */
    ANode deleteCard(ANode n, int A, int H, long ord){
        if(n==null) return null;
        if(A < n.A) n.left = deleteCard(n.left,A,H,ord);
        else if(A > n.A) n.right = deleteCard(n.right,A,H,ord);
        else{
            n.bucket.root = n.bucket.delete(n.bucket.root, H, ord);
            if(n.bucket.root == null){
                if(n.left==null || n.right==null) n = (n.left!=null)?n.left:n.right;
                else {
                    ANode m = minNode(n.right);
                    n.A = m.A; n.bucket = m.bucket;
                    n.right = deleteWholeANode(n.right, m.A);
                }
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

    public void insert(Card c){ root = insert(root, c.Acur, c); }
    public void remove(Card c){ root = deleteCard(root, c.Acur, c.Hcur, c.orderId); }

    // ===== Priority Searches =====
    /** Finds best card for Priority 1 rule (kill & survive). */
    public Card findPriority1(int Hstr, int Astr){ return p1(root, Hstr, Astr); }

    /**Traverses nodes by decreasing attack and checks buckets for the smallest H > Astr. */
    Card p1(ANode n, int Hstr, int Astr){
        if(n==null) return null;
        if(maxH(n) <= Astr) return null;  // no surviving candidate here

        // Search left subtree first (smaller A, but possible candidate)
        boolean leftOk = (n.left!=null) && (maxA(n.left) >= Hstr) && (maxH(n.left) > Astr);
        if(leftOk){
            Card t = p1(n.left,Hstr,Astr);
            if(t!=null) return t;
        }

        // Check current node: same A bucket
        if(n.A >= Hstr && HTree.hasHGreaterThan(n.bucket.root, Astr)){
            Card c = n.bucket.minHGreaterThan(Astr);
            if(c!=null) return c;
        }

        // Continue to right if needed
        boolean rightOk = (n.right!=null) && (maxH(n.right) > Astr);
        if(rightOk) return p1(n.right,Hstr,Astr);
        return null;
    }

    /** Finds best card for Priority 2 rule (survive but not kill). */
    public Card findPriority2(int Hstr, int Astr){ return p2(root, Hstr, Astr); }

    /** Traverses right-to-left (since smaller A may still satisfy survival). */
    Card p2(ANode n, int Hstr, int Astr){
        if(n==null) return null;
        if(maxH(n) <= Astr) return null;

        // Right subtree first (larger A closer to Hstr boundary)
        boolean rightOk = (n.right!=null) && (minA(n.right) <= Hstr) && (maxH(n.right) > Astr);
        if(rightOk){
            Card r = p2(n.right,Hstr,Astr);
            if(r!=null) return r;
        }

        // Current node check
        if(n.A <= Hstr && HTree.hasHGreaterThan(n.bucket.root, Astr)){
            Card c = n.bucket.minHGreaterThan(Astr);
            if(c!=null) return c;
        }

        // Left subtree fallback
        boolean leftOk = (n.left!=null) && (minA(n.left) <= Hstr) && (maxH(n.left) > Astr);
        if(leftOk) return p2(n.left,Hstr,Astr);
        return null;
    }

    /** Finds best card for Priority 3 rule (kill but not survive). */
    public Card findPriority3(int Hstr, int Astr){ return p3(root, Hstr, Astr); }

    /** Chooses smallest A that meets killing condition; inside bucket, lowest H that dies. */
    Card p3(ANode n, int Hstr, int Astr){
        if(n == null) return null;

        // Check left subtree first for smaller A candidates
        boolean leftMay = (n.left != null)  && (maxA(n.left) >= Hstr) && (minH(n.left) <= Astr);
        if(leftMay){
            Card L = p3(n.left, Hstr, Astr);
            if(L != null) return L;
        }

        // Current node: A ≥ Hstr, find smallest H ≤ Astr in this bucket
        if(n.A >= Hstr && HTree.hasHLessOrEq(n.bucket.root, Astr)){
            Card c = n.bucket.minHLessOrEq(Astr);
            if(c != null) return c;
        }

        // Continue right if no candidate found yet
        boolean rightMay = (n.right != null) && (maxA(n.right) >= Hstr) && (minH(n.right) <= Astr);
        if(rightMay) return p3(n.right, Hstr, Astr);

        return null;
    }

    /** Finds strongest remaining card (Priority 4). */
    public Card findPriority4(){ return p4(root); }

    /** Within that A, choose the one with minimal Hcur (ties: earliest order). */
    Card p4(ANode n){
        if(n == null) return null;

        // Go as right as possible → largest A first
        if(n.right != null){
            Card r = p4(n.right);
            if(r != null) return r;
        }

        // In this bucket, choose lowest health card (arbitrary damage)
        Card here = n.bucket.minHLessOrEq(Integer.MAX_VALUE);
        if(here != null) return here;

        // Fallback to left subtree if no card in this bucket
        if(n.left != null) return p4(n.left);
        return null;
    }

    /** Finds first stealable card. */
    public Card stealFirstGreaterAWithH(int aLim, int hLim){ return stealRec(root, aLim, hLim, null); }

    /**
     * Steal operation: Find the first card with A > aLim and H > hLim.
     * Traverses in-order, always choosing the smallest valid A and H.
     */
    Card stealRec(ANode n, int aLim, int hLim, Card best){
        if(n==null) return best;

        // Skip left if A ≤ limit
        if(n.A <= aLim) return stealRec(n.right, aLim, hLim, best);

        // Try smaller A values first
        best = stealRec(n.left, aLim, hLim, best);
        if(best!=null) return best;

        // Check this bucket
        if(HTree.maxH(n.bucket.root) > hLim){
            Card c = n.bucket.minHGreaterThan(hLim);
            if(c!=null) return c;
        }

        // Otherwise, continue right
        return stealRec(n.right, aLim, hLim, best);
    }
}
