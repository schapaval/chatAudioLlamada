//
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

public final class ChatHolder extends Ice.ObjectHolderBase<Chat>
{
    public
    ChatHolder()
    {
    }

    public
    ChatHolder(Chat value)
    {
        this.value = value;
    }

    public void
    valueReady(Ice.Object v)
    {
        if(v == null || v instanceof Chat)
        {
            value = (Chat)v;
        }
        else
        {
            IceInternal.Ex.throwUOE(type(), v);
        }
    }

    public String
    type()
    {
        return _ChatDisp.ice_staticId();
    }
}
