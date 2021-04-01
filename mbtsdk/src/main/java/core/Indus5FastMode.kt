package core

/**
 * TODO: remove this later, this class is used to quick develop function of Q+ indus5
 */
object Indus5FastMode {

    private var isMelomindIndus5 = false

    fun setMelomindIndus5() {
        isMelomindIndus5 = true
    }

    fun isEnabled(): Boolean {
        return isMelomindIndus5
    }
}