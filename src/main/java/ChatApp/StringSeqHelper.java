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

public final class StringSeqHelper
{
    public static void
    write(Ice.OutputStream ostr, String[] v)
    {
        ostr.writeStringSeq(v);
    }

    public static String[]
    read(Ice.InputStream istr)
    {
        String[] v;
        v = istr.readStringSeq();
        return v;
    }
}
