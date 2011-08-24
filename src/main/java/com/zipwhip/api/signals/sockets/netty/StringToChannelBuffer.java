package com.zipwhip.api.signals.sockets.netty;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.nio.charset.Charset;

public class StringToChannelBuffer extends OneToOneEncoder implements ChannelHandler {

    private static Logger logger = Logger.getLogger(StringToChannelBuffer.class);

    static final String CRLF = new String(new byte[] { 13, 10 }); // \x0D\x0A

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

        if (!(msg instanceof String)) {
            return msg;
        }

        msg = msg + CRLF;

        logger.debug("StringToChannelBuffer.encode: " + msg);

        return ChannelBuffers.copiedBuffer((String) msg, Charset.defaultCharset());
    }

}
