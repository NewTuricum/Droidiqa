package ch.newturicum.droidiqa.network

import ch.newturicum.droidiqa.Constants
import ch.newturicum.droidiqa.Constants.ZIL_DEFAULT_GAS_LIMIT_SC
import ch.newturicum.droidiqa.database.dao.ZilAccountEntity
import ch.newturicum.droidiqa.network.request.SingleTransaction
import ch.newturicum.droidiqa.network.request.TransactionData
import ch.newturicum.droidiqa.security.KeyEncoder
import ch.newturicum.droidiqa.transitions.Transition
import ch.newturicum.droidiqa.transitions.TransitionParameter
import ch.newturicum.droidiqa.util.DroidiqaUtils
import ch.newturicum.droidiqa.util.extensions.pack
import com.google.gson.Gson

internal class TransactionFactory {

    companion object {

        fun zilliqaTransfer(
            networkId: Int,
            amount: Double,
            senderAccount: ZilAccountEntity,
            receiverAddress: String,
            gasPrice: Long,
            keyEncoder: KeyEncoder
        ): SingleTransaction {
            return buildTransaction(
                networkId = networkId,
                amount = amount,
                senderAccount = senderAccount,
                destAddress = receiverAddress,
                gasPrice = gasPrice,
                code = "",
                data = null,
                keyEncoder = keyEncoder
            )
        }

        fun smartContractCall(
            networkId: Int,
            amount: Double,
            senderAccount: ZilAccountEntity,
            contractAddress: String,
            gasPrice: Long,
            transition: Transition,
            keyEncoder: KeyEncoder
        ): SingleTransaction {
            val senderAddress = DroidiqaUtils.fromBech32Address(senderAccount.address)
            return buildTransaction(
                networkId = networkId,
                amount = amount,
                senderAccount = senderAccount,
                destAddress = contractAddress,
                gasPrice = gasPrice,
                code = "",
                data = transition.toTransactionData(senderAccount.address),
                keyEncoder = keyEncoder
            )
        }

        fun smartContractCall(
            networkId: Int,
            amount: Double,
            senderAccount: ZilAccountEntity,
            contractAddress: String,
            gasPrice: Long,
            method: String,
            parameters: Array<TransitionParameter>,
            keyEncoder: KeyEncoder
        ): SingleTransaction {
            val senderAddress = DroidiqaUtils.fromBech32Address(senderAccount.address)
            return buildTransaction(
                networkId = networkId,
                amount = amount,
                senderAccount = senderAccount,
                destAddress = contractAddress,
                gasPrice = gasPrice,
                code = "",
                data = TransactionData(method, "0", senderAddress, senderAddress, parameters),
                keyEncoder = keyEncoder
            )
        }

        private fun buildTransaction(
            networkId: Int,
            amount: Double,
            senderAccount: ZilAccountEntity,
            destAddress: String,
            gasPrice: Long,
            code: String,
            data: TransactionData?,
            keyEncoder: KeyEncoder
        ): SingleTransaction {
            val theData = if (data == null) "" else Gson().toJson(data)
            val transaction =
                com.firestack.laksaj.transaction.Transaction.builder()
                    .amount(DroidiqaUtils.zilToQa(amount).toString())
                    .senderPubKey(senderAccount.publicKey.lowercase())
                    .nonce((senderAccount.nonce).toString())
                    .gasLimit(ZIL_DEFAULT_GAS_LIMIT_SC)
                    .gasPrice(gasPrice.toString())
                    .toAddr(destAddress)
                    .data(theData)
                    .code(code.replace("/\\", ""))
                    .version(networkId.pack(Constants.ZIL_MESSAGE_VERSION).toString())
                    .build()
            senderAccount.sign(transaction, keyEncoder)

            return SingleTransaction(
                version = transaction.version.toInt(),
                toAddr = transaction.toAddr.replace("0x", ""),
                amount = transaction.amount,
                nonce = transaction.nonce.toInt(),
                pubKey = transaction.senderPubKey.lowercase(),
                gasPrice = transaction.gasPrice,
                code = code,
                data = theData,
                gasLimit = transaction.gasLimit,
                signature = transaction.signature
            )
        }
    }
}