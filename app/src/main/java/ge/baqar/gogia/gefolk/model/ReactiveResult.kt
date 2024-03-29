package ge.baqar.gogia.gefolk.model

sealed class ReactiveResult<out L, out R>
data class FailedResult<out L>(val value: L) : ReactiveResult<L, Nothing>()
data class SucceedResult<out R>(val value: R) : ReactiveResult<Nothing, R>()

val <T> T.asError: FailedResult<T> get() = FailedResult(this)

val <T> T.asSuccess: SucceedResult<T> get() = SucceedResult(this)