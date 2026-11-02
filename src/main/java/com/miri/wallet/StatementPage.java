package com.miri.wallet;

import java.util.List;

public record StatementPage(List<StatementLine> items, int page, int size,
                            long totalItems, int totalPages) {
}
