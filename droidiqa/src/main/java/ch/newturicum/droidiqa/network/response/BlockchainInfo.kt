package ch.newturicum.droidiqa.network.response

internal data class ShardingStructure(
    val NumPeers: List<Int>
)

internal data class BlockchainInfoResult(
    val CurrentDSEpoch: Int,
    val CurrentMiniEpoch: Int,
    val DSBlockRate: Double,
    val NumDSBlocks: Int,
    val NumPeers: Int,
    val NumTransactions: Int,
    val NumTxBlocks: Int,
    val NumTxnsDSEpoch: Int,
    val NumTxnsTxEpoch: Int,
    val ShardingStructure: ShardingStructure
)

internal data class BlockchainInfo(
    val TransactionRate: Double,
    val TxBlockRate: Double
) : BaseDto<BlockchainInfoResult>()