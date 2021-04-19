package mbtsdk.com.mybraintech.sdkv2

fun ArrayList<ArrayList<Float>>.toChannels12(): ArrayList<ArrayList<Float>> {
    val origin = this
    val result = arrayListOf<ArrayList<Float>>()
    for (i in 0 until 2) {
        val row = arrayListOf<Float>(origin[i][0], origin[i][1])
        result.add(row)
    }
    return result
}

fun ArrayList<ArrayList<Float>>.toChannels34(): ArrayList<ArrayList<Float>> {
    val origin = this
    val result = arrayListOf<ArrayList<Float>>()
    for (i in 0 until origin.size) {
        val row = arrayListOf<Float>(origin[i][2], origin[i][3])
        result.add(row)
    }
    return result
}