package hu.financial.dto.report;

import java.util.List;

public record TrendResponseDto(String month, int months, List<TrendPointDto> points) {
}
