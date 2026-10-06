package io.github.devssw.springquant.market.candledata;

import io.github.devssw.springquant.market.Candle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// CSV 차트 데이터를 읽어서 정리합니다.
public final class CandleCsvReader {
    private CandleCsvReader() { }
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 앞 뒤 공백 제거, 큰 따옴표 제거
    private static String clean(String value) {
        String cleaned = value.strip();

        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            return cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    // CSV 첫 줄의 내용 timestamp,open,high,low,close,volume
    private static Map<String, Integer> createHeaderIndexes(String headerLine) {
        // 첫 줄 내용을 쉼표를 기준으로 분리 후 저장
        String[] headers = headerLine.split(",", -1);
        // <open, 0>과 같이 저장
        Map<String, Integer> indexes = new HashMap<>();

        // BOM 문자 제거, 영문 소문자 변환 등의 정리를 수행 (내부적으로 clean을 통한 정리도 포함)
        for (int i = 0; i < headers.length; i++) {
            String header = clean(headers[i]).replace("\uFEFF", "").toLowerCase(Locale.ROOT);
            // 정리 이후 위치를 저장 (만약 열의 순서가 open, timestamp, high, close, .. 이런 식으로 순서가 바뀌어 있어도 읽을 수 있도록 고정 인덱스를 지정)
            indexes.put(header, i);
        }
        return indexes;
    }

    // 필수 열이 존재하는지 확인하고, 그 열의 인덱스 위치를 반환
    private static int requireColumn(Map<String, Integer> indexes, String columnName) {
        // 위의 Map 정리한 대로 위치가 반환됨 ( columnName이 timestamp라면 Map에서 해당 String을 가진 인덱스 위치를 반환함)
        Integer index = indexes.get(columnName);
        if (index == null) {
            throw new IllegalArgumentException("필수 CSV 컬럼이 NULL입니다. - " + columnName);
        }
        // 열의 번호 리턴
        return index;
    }

    // 현재 CSV 행에서 지정한 열의 문자열 값을 가져옴 (행이 1725148800000,59000,59500,58800,59300 이고, index로 2를 선택하면, 59500을 가져옴)
    private static String getValue(String[] values, int index, int lineNumber) {
        // lineNumber는 오류가 난 행을 알려주기 위해 입력값으로 받아옴
        if (index >= values.length) {
            throw new IllegalArgumentException("해당 라인에서 CSV 값이 없습니다. - " + lineNumber);
        }
        // 값을 정리해서 반환합니다 (공백과 큰따옴표)
        return clean(values[index]);
    }

    // 문자열로 된 가격을 double 타입으로 변환합니다
    private static double parsePrice(String value, String columnName, int lineNumber) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException(columnName + "은 유한한 숫자여야 합니다. - " + lineNumber);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("잘못된 형식입니다 - " + columnName + " at line " + lineNumber + ": " + value, exception);
        }
    }

    // 여러 형태로 된 시간 값을 openTimeMs에 사용할 밀리초 정수로 변환 ( 최종 반환형은 long)
    private static long parseTimestamp(String value, int lineNumber) {
        // 숫자 시간인 경우 시도
        try {
            // 시간이 숫자인지 확인
            double numericTimestamp = Double.parseDouble(value);
            // 유한한 숫자인지 확인
            if (!Double.isFinite(numericTimestamp)) {
                throw new NumberFormatException();
            }
            // 밀리초 인 경우 ("1725148800000") -> 그대로 사용
            if (Math.abs(numericTimestamp) >= 1_000_000_000_000.0) {
                return Math.round(numericTimestamp);
            }
            // 초 인 경우 ("1725148800") -> 1000을 곱해서 밀리초로 사용
            return Math.round(numericTimestamp * 1000.0);
        } catch (NumberFormatException ignored) {
            // 아닌 경우 아래로 시도
        }

        // UTC ISO 형식인 경우 시도 ("2024-09-01T00:00:00Z") -> 밀리초로 변환해서 사용 (Z는 UTC를 의미)
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // 아닌 경우 아래로 시도
        }

        // 시간대가 포함된 형식인 경우 시도 ("2024-09-01T09:00:00+09:00") -> 밀리초로 변환해서 사용 (+09:00은 UTC 기준 9시간 차이를 뜻함)
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // 아닌 경우 아래로 시도
        }

        // 시간대가 없는 형식인 경우 시도 ("2024-09-01 00:00:00") -> 위에서 정의한 포맷 형식으로 바꾸고, 밀리초로 변환하여 사용
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMAT);
            return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeParseException exception) {
            // 위의 모든 경우에서 변환을 실패했기에 예외를 발생시킴
            throw new IllegalArgumentException("잘못된 형식의 시간입니다. - " + lineNumber + ": " + value, exception);
        }
    }
    // path로 파일 위치를 받습니다.
    public static List<Candle> read(Path path) throws IOException {
        Objects.requireNonNull(path, "경로가 NULL입니다.");
        List<Candle> candles = new ArrayList<>();

        // 헤더를 읽습니다
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV 헤더가 없습니다.");
            }

            // 열 위치 찾기
            Map<String, Integer> headerIndexes = createHeaderIndexes(headerLine);

            // OHLC 읽고 각 인덱스 저장
            int timestampIndex = requireColumn(headerIndexes, "timestamp");
            int openIndex = requireColumn(headerIndexes, "open");
            int highIndex = requireColumn(headerIndexes, "high");
            int lowIndex = requireColumn(headerIndexes, "low");
            int closeIndex = requireColumn(headerIndexes, "close");
            int lineNumber = 1;
            String line;

            // 각 행 읽기(위에서 첫 번쨰 행을 읽었으니, 두 번째 행부터 마지막 줄까지 하나씩 읽기)
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                // 빈 줄 무시
                if (line.isBlank()) {
                    continue;
                }
                // 각 행은 쉼표로 분리해서 저장
                String[] values = line.split(",", -1);

                long openTimeMs = parseTimestamp(getValue(values, timestampIndex, lineNumber), lineNumber);
                double open = parsePrice(getValue(values, openIndex, lineNumber), "open", lineNumber);
                double high = parsePrice(getValue(values, highIndex, lineNumber), "high", lineNumber);
                double low = parsePrice(getValue(values, lowIndex, lineNumber), "low", lineNumber);
                double close = parsePrice(getValue(values, closeIndex, lineNumber), "close", lineNumber);
                candles.add(new Candle(openTimeMs, open, high, low, close));
            }
        }
        candles.sort(Comparator.comparingLong(Candle::openTimeMs));
        return List.copyOf(candles);
    }
}
