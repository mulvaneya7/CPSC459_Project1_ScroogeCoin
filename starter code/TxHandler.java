import java.util.ArrayList;
import java.util.List;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	//this currenct utxoPool
	private UTXOPool thisUTXOPool;
	private RSAKey thisRSAKey;

	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		//assign the utxoPool on creation
		thisUTXOPool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		ArrayList<UTXO> txInputs = new ArrayList<UTXO>();

		//total INPUT and OUTPUT values
		double totalInput = 0.0;
		double totalOutput = 0.0;

		//(1) all outputs claimed by tx are in the current UTXO pool
		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input input = tx.getInput(i);
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			if(!thisUTXOPool.contains(utxo)) {
				return false;
			}

			//(2) the signatures on each input of tx are valid
			Transaction.Output output = thisUTXOPool.getTxOutput(utxo);
			//	Give the RSA key the publicKey
			thisRSAKey = output.address;

			// tx.getRawDataToSign(index) -> message
			// input.signature -> signature from corresponding transaction input
			if(!thisRSAKey.verifySignature(tx.getRawDataToSign(i), input.signature)) {
				return false;
			}

			//(3) No UTXO is claimed multiple times by tx
			if (txInputs.contains(utxo)) {
				return false;
				
			} else {
				txInputs.add(utxo);
			}

			//(5) the sum of tx's input values is greater than or equal to the sum of its output values, false otherwise
			//this will add them together as we are looping through inputs to be used later
			totalInput += thisUTXOPool.getTxOutput(utxo).value;

		}

		//(4) all of tx's output values are non-negative
		for(int i = 0; i < tx.numOutputs(); i++) {
			if(tx.getOutput(i) < 0.0) {
				return false;
			}
			//(5) collecting more data to verify (5)
			totalOutput += tx.getOutput(i).value;
		}


		//(5) cont...
		if(totalInput < totalOutput) {
			return false;
		}

		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		List<Transaction> acceptedTx = new ArrayList<Transaction>();
		for (int i = 0; i < possibleTxs.length; i++) {
			Transaction tx = possibleTxs[i];
			if (isValidTx(tx)) {
				acceptedTx.add(tx);

				List<Transaction.Output> outputs = tx.getOutputs();
				for (int j = 0; j < outputs.size(); j++) {
					Transaction.Output output = outputs.get(j);
					UTXO utxo = new UTXO(tx.getHash(), j);
					thisUTXOPool.addUTXO(utxo, output);
				}

				List<Transaction.Input> inputs = tx.getInputs();
				for (int j = 0; j < inputs.size(); j++) {
					Transaction.Input input = inputs.get(j);
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					thisUTXOPool.removeUTXO(utxo);
				}
			}
		}
		Transaction[] result = new Transaction[acceptedTx.size()];
		acceptedTx.toArray(result);
		return result;
	}

} 
