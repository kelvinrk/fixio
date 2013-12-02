/*
 * Copyright 2013 The FIX.io Project
 *
 * The FIX.io Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package fixio.netty.pipeline;

import fixio.fixprotocol.*;
import fixio.fixprotocol.session.FixSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;

import java.util.List;

public abstract class AbstractSessionHandler extends MessageToMessageCodec<FixMessage, FixMessage> {

    public static final AttributeKey<FixSession> FIX_SESSION_KEY = AttributeKey.valueOf("fixSession");

    protected void updateFixMessageHeader(ChannelHandlerContext ctx, FixMessage response) {
        getSession(ctx).prepareOutgoing(response);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Attribute<FixSession> fixSessionAttribute = ctx.attr(FIX_SESSION_KEY);
        FixSession session = fixSessionAttribute.getAndRemove();
        getLogger().info("Fix Session Closed. {}", session);
    }

    /**
     * Retrieves {@link FixSession} from context.
     *
     * @return null if session not established.
     */
    protected FixSession getSession(ChannelHandlerContext ctx) {
        Attribute<FixSession> fixSessionAttribute = ctx.attr(FIX_SESSION_KEY);
        return fixSessionAttribute.get();
    }

    protected boolean setSession(ChannelHandlerContext ctx, FixSession fixSession) {
        assert (fixSession != null) : "Parameter 'fixSession' expected.";
        Attribute<FixSession> fixSessionAttribute = ctx.attr(FIX_SESSION_KEY);
        return fixSessionAttribute.compareAndSet(null, fixSession);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, List<Object> out) throws Exception {
        updateFixMessageHeader(ctx, msg);
        getLogger().debug("Sending outbound: {}", msg);
        out.add(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FixMessage msg, List<Object> out) throws Exception {
        FixSession session = getSession(ctx);
        if (session == null) {
            getLogger().error("Session not established. Skipping message: {}", msg);
            ctx.channel().close();
            return;
        }

        FixMessageHeader header = msg.getHeader();

        final int msgSeqNum = header.getMsgSeqNum();
        if (!session.checkIncomingSeqNum(msgSeqNum)) {
            getLogger().error("MessageSeqNum={} != expected {}.", msgSeqNum, session.getNextIncomingMessageSeqNum());
        }
        out.add(msg);
    }

    protected abstract Logger getLogger();

    protected void sendReject(ChannelHandlerContext ctx, FixMessage originalMsg, boolean closeConnection) {
        final SimpleFixMessage reject = createReject(originalMsg);

        ctx.writeAndFlush(reject);
    }

    protected SimpleFixMessage createReject(FixMessage originalMsg) {
        final SimpleFixMessage reject = new SimpleFixMessage(MessageTypes.REJECT);
        reject.add(FieldType.RefSeqNum, originalMsg.getInt(FieldType.MsgSeqNum.tag()));
        reject.add(FieldType.RefMsgType, originalMsg.getMessageType());
        return reject;
    }

}