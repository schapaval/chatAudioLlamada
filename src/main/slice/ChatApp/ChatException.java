// Copyright (c) ZeroC, Inc. All rights reserved.
//
//
// Ice version 3.7.10
//
// <auto-generated>
//
// Generated from file `Chat.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package ChatApp;

public class ChatException extends com.zeroc.Ice.UserException
{

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }
    public ChatException()
    {
        this.reason = "";
    }

    public ChatException(Throwable cause)
    {
        super(cause);
        this.reason = "";
    }

    public ChatException(String reason)
    {
        this.reason = reason;
    }

    public ChatException(String reason, Throwable cause)
    {
        super(cause);
        this.reason = reason;
    }

    @Override
    public String ice_id()
    {
        return "::ChatApp::ChatException";
    }

    public String reason;

    /** @hidden */
    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::ChatApp::ChatException", -1, true);
        ostr_.writeString(reason);
        ostr_.endSlice();
    }

    /** @hidden */
    @Override
    protected void _readImpl(com.zeroc.Ice.InputStream istr_)
    {
        istr_.startSlice();
        reason = istr_.readString();
        istr_.endSlice();
    }

    /** @hidden */
    public static final long CHAT_EXCEPTION_SERIAL_VERSION_UID = -1254534298020021442L;
}
