import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private BlockNode maxHeightNode;
    private TransactionPool transactionPool;
    private HashMap<ByteArrayWrapper, BlockNode> blockChain;

    // all information required in handling a block in block chain
    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool for making a new block on top of this block
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    /**
     * create an empty block chain with just a genesis block.
     * Assume genesis block is a valid block
     */
    public BlockChain(Block genesisBlock) {
        blockChain = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();
        transactionPool = new TransactionPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); ++i) {
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), coinbase.getOutput(i));
        }
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        maxHeightNode = genesisNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightNode.b;
    }

    /**
     * Get the UTXOPool for mining a new block on top of
     * max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.getUTXOPoolCopy();
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
    public boolean addBlock(Block b) {
        if (b == null) {
            return false;
        }

        byte[] prevBlockHash = b.getPrevBlockHash();
        if (prevBlockHash == null) {
            return false;
        }

        BlockNode parentBlockNode = blockChain.get(new ByteArrayWrapper(prevBlockHash));
        if (parentBlockNode == null) {
            return false;
        }

        // Block validation
        int proposedHeight = parentBlockNode.height + 1;
        if (proposedHeight <= maxHeightNode.height - CUT_OFF_AGE) {
            return false;
        }

        // Transaction validation
        TxHandler handler = new TxHandler(parentBlockNode.getUTXOPoolCopy());
        Transaction[] txs = b.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);
        
        if (validTxs.length != txs.length) {
            return false;
        }

        for (Transaction tx : txs){
            transactionPool.removeTransaction(tx.getHash());
        }

        UTXOPool utxoPool = handler.getUTXOPool();
    
        // Add coinbase transactions to the UTXO pool;
        Transaction coinBaseTx = b.getCoinbase();
		int numOutputs = coinBaseTx.numOutputs();
		for (int i = 0; i < numOutputs; ++i) {
			UTXO utxo = new UTXO(coinBaseTx.getHash(), i);
			utxoPool.addUTXO(utxo, coinBaseTx.getOutput(i));
		}

        // Add to the blockchain
        BlockNode node = new BlockNode(b, parentBlockNode, utxoPool);
        blockChain.put(new ByteArrayWrapper(b.getHash()), node);
        if (proposedHeight > maxHeightNode.height) {
            maxHeightNode = node;
        }

        return true;
    }

    /** Add a transaction in transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }
}