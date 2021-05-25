package indus5

import model.Position3D

class ArrayHelper {
    
    companion object {
        /**
         * TODO: use System.arrayCopy to enhance performance
         */
        @JvmStatic
        fun copyArray(list: ArrayList<Position3D>) : ArrayList<Position3D> {
            val result = arrayListOf<Position3D>()
//            System.arraycopy(list, 0, result, 0, list.size)
            result.addAll(list)
            return result;
        }
    }
}