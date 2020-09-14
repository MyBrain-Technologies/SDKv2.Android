package core

interface Completable<Input, Error> {
    var onSuccess: ((Input) -> Unit)?
    var onFailure: ((Error) -> Unit)?
}