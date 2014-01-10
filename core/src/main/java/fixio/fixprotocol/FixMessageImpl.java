/*
 * Copyright 2014 The FIX.io Project
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

package fixio.fixprotocol;

import java.util.LinkedList;
import java.util.List;

public class FixMessageImpl implements FixMessage {

    private final FixMessageHeader header = new FixMessageHeader();
    private final FixMessageTrailer trailer = new FixMessageTrailer();
    private final List<FixMessageFragment> body = new LinkedList<>();

    public FixMessageImpl() {
    }

    public FixMessageImpl(String messageType) {
        header.setMessageType(messageType);
    }

    public FixMessageImpl add(FieldType field, int value) {
        assert (field != null) : "Tag must be specified.";
        return add(field, String.valueOf(value));
    }

    public FixMessageImpl add(int tagNum, int value) {
        assert (tagNum > 0) : "Tag must be positive.";
        return add(tagNum, String.valueOf(value));
    }

    public FixMessageImpl add(FieldType field, String value) {
        assert (field != null) : "Tag must be specified.";
        assert (value != null) : "Value must be specified.";
        return add(field.tag(), value);
    }

    public FixMessageImpl add(int tagNum, String value) {
        assert (tagNum > 0) : "TagNum must be positive. Got " + tagNum;
        assert (value != null) : "Value must be specified.";
        FieldType fieldType = FieldType.forTag(tagNum);
        switch (fieldType) {
            case BeginString:
                header.setBeginString(value.intern());
                break;
            case CheckSum:
                trailer.setCheckSum(Integer.parseInt(value));
                break;
            case SenderCompID:
                header.setSenderCompID(value);
                break;
            case TargetCompID:
                header.setTargetCompID(value);
                break;
            case MsgSeqNum:
                header.setMsgSeqNum(Integer.parseInt(value));
                break;
            case MsgType:
                header.setMessageType(value.intern());
                break;
            default:
                body.add(new Field(tagNum, value));
                break;
        }
        return this;
    }

    @Override
    public List<FixMessageFragment> getBody() {
        return body;
    }

    @Override
    public String getString(int tagNum) {
        FixMessageFragment item = getFirst(tagNum);
        if (item == null) {
            return null;
        }
        if (item instanceof Field) {
            return ((Field) item).getValue();
        } else {
            throw new IllegalArgumentException("Tag " + tagNum + " is not a Field.");
        }

    }

    @Override
    public String getString(FieldType field) {
        return getString(field.tag());
    }

    @Override
    public Integer getInt(int tagNum) {
        String s = getString(tagNum);
        if (s != null) {
            return Integer.parseInt(s);
        }
        return null;
    }

    @Override
    public Integer getInt(FieldType field) {
        return getInt(field.tag());
    }

    @Override
    public FixMessageHeader getHeader() {
        return header;
    }

    public int getMsgSeqNum() {
        return header.getMsgSeqNum();
    }

    @Override
    public String getMessageType() {
        return header.getMessageType();
    }

    public void setMessageType(String messageType) {
        header.setMessageType(messageType);
    }

    public int getChecksum() {
        return trailer.getCheckSum();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FixMessageImpl{");
        sb.append("header=").append(header);
        sb.append(", body=").append(body);
        sb.append(", trailer=").append(trailer);
        sb.append('}');
        return sb.toString();
    }

    public List<Group> getGroups(int tagNum) {
        FixMessageFragment fragment = getFirst(tagNum);
        if (fragment instanceof GroupField) {
            return ((GroupField) fragment).getGroups();
        }
        return null;
    }

    private FixMessageFragment getFirst(int tagNum) {
        for (FixMessageFragment item : body) {
            if (item.getTagNum() == tagNum) {
                return item;
            }
        }
        return null;
    }
}
