package core.synchronisation;

interface IStreamer<U>{

    void stream(U message);

    U initStreamRequest(String address, Object... dataToStream);
        
}