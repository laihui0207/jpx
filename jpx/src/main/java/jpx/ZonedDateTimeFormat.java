/*
 * Java GPX Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package jpx;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Enumeration of the valid date time formats.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
enum ZonedDateTimeFormat {

	// http://books.xmlschemata.org/relaxng/ch19-77049.html

	ISO_DATE_TIME_UTC(
		new DateTimeFormatterBuilder()
			.append(ISO_LOCAL_DATE_TIME)
			.optionalStart()
			.appendOffsetId()
			.toFormatter()
			.withResolverStyle(ResolverStyle.LENIENT)
			.withZone(UTC),
		"^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})*+Z*+$"
	),

	ISO_DATE_TIME_OFFSET(
		new DateTimeFormatterBuilder()
			.append(ISO_LOCAL_DATE_TIME)
			.optionalStart()
			.appendOffsetId()
			.toFormatter(),
		"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})*+([+-]\\d{2}:\\d{2})"
	);

	// Default formatter used for formatting time strings.
	private static final DateTimeFormatter FORMATTER =
		DateTimeFormatter.ISO_DATE_TIME.withZone(UTC);

	private final DateTimeFormatter _formatter;
	private final Pattern[] _patterns;

	private ZonedDateTimeFormat(final DateTimeFormatter formatter, final String... patterns) {
		_formatter = requireNonNull(formatter);
		_patterns = Stream.of(patterns)
			.map(Pattern::compile)
			.toArray(Pattern[]::new);
	}

	private boolean matches(final String time) {
		return Stream.of(_patterns)
			.anyMatch(p -> p.matcher(time).matches());
	}

	/**
	 * Parses the given time string with the current formatter.
	 *
	 * @param time the time string to pare
	 * @return the parsed zoned date time
	 *  @throws DateTimeParseException if the text cannot be parsed
	 */
	public ZonedDateTime formatParse(final String time) {
		return time != null ? ZonedDateTime.parse(time, _formatter) : null;
	}

	/**
	 * Return the default format of the given {@code ZonedDateTime}.
	 *
	 * @param time the {@code ZonedDateTime} to format
	 * @return the time string of the given zoned date time
	 */
	public static String format(final ZonedDateTime time) {
		return time != null ? FORMATTER.format(time) : null;
	}

	public static ZonedDateTime parse(final String time) {
		return time != null
			? parseOptional(time).orElseThrow(() ->
				new IllegalArgumentException(
					String.format("Can't parse time: %s'", time)))
			: null;
	}

	public static Optional<ZonedDateTime> parseOptional(final String time) {
		return findFormat(time)
			.map(tf -> tf.formatParse(time));
	}

	/**
	 * Finds the formatter which fits the given {@code time} string.
	 *
	 * @param time the time string
	 * @return the formatter which fits to the given {@code time} string, or
	 *         {@code Optional.empty()} of no formatter is found
	 */
	static Optional<ZonedDateTimeFormat> findFormat(final String time) {
		return Stream.of(values())
			.filter(tf -> tf.matches(time))
			.findFirst();
	}

	/**
	 * Tests if the given date times represents the same point on the time-line.
	 *
	 * @param a the first date time
	 * @param b the second date time
	 * @return {@code true} if the two date times represents the same point on
	 *         the time-line, {@code false} otherwise
	 */
	static boolean equals(final ZonedDateTime a, final ZonedDateTime b) {
		final Instant i1 = a != null ? a.toInstant() : null;
		final Instant i2 = b != null ? b.toInstant() : null;
		return Objects.equals(i1, i2);
	}

}
