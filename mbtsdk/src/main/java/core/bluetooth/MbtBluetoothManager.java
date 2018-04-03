package core.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Etienne on 08/02/2018.
 * The bluetooth manager that will manage the whole bluetooth communication and configuration throughout
 * the lifetime of this instance
 */

public final class MbtBluetoothManager {

    private Context mContext;

    public MbtBluetoothManager(@NonNull Context context){
        //save client side objects in variables
        mContext = context;
    }

    public boolean connect(){
        return false;
    }

}
