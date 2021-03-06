/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.Log;
import org.traccar.helper.StringFinder;
import org.traccar.model.Position;

public class EnforaProtocolDecoder extends BaseProtocolDecoder {

    public EnforaProtocolDecoder(EnforaProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = Pattern.compile(
            "GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2}).(\\d+)," + // Time (HHMMSS.SS)
            "([AV])," +                  // Validity
            "(\\d{2})(\\d{2}.\\d+)," +   // Latitude (DDMM.MMMMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}.\\d+)," +   // Longitude (DDDMM.MMMMMM)
            "([EW])," +
            "(\\d+.\\d+)?," +            // Speed
            "(\\d+.\\d+)?," +            // Course
            "(\\d{2})(\\d{2})(\\d{2})," + // Date (DDMMYY)
            ".*[\r\n\u0000]*");

    public static final int IMEI_LENGTH = 15;

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        // Find IMEI (Modem ID)
        String imei = null;
        for (int first = -1, i = 0; i < buf.readableBytes(); i++) {
            if (!Character.isDigit((char) buf.getByte(i))) {
                first = i + 1;
            }

            // Found digit string
            if (i - first == IMEI_LENGTH - 1) {
                imei = buf.toString(first, IMEI_LENGTH, Charset.defaultCharset());
                break;
            }
        }

        // Write log
        if (imei == null) {
            Log.warning("Enfora decoder failed to find IMEI");
            return null;
        }

        // Find GPSMC string
        int start = buf.indexOf(buf.readerIndex(), buf.writerIndex(), new StringFinder("GPRMC"));
        if (start == -1) {
            // Message does not contain GPS data
            return null;
        }
        String sentence = buf.toString(start, buf.readableBytes() - start, Charset.defaultCharset());

        // Parse message
        Matcher parser = PATTERN.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        Integer index = 1;

        // Get device by IMEI
        if (!identify(imei, channel)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.parseInt(parser.group(index++)) * 10);

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);

        // Latitude
        double latitude = Double.parseDouble(parser.group(index++));
        latitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
        position.setLatitude(latitude);

        // Longitude
        double longitude = Double.parseDouble(parser.group(index++));
        longitude += Double.parseDouble(parser.group(index++)) / 60;
        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
        position.setLongitude(longitude);

        // Speed
        position.setSpeed(Double.parseDouble(parser.group(index++)));

        // Course
        String course = parser.group(index++);
        if (course != null) {
            position.setCourse(Double.parseDouble(course));
        }

        // Date
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
        position.setTime(time.getTime());
        return position;
    }

}
