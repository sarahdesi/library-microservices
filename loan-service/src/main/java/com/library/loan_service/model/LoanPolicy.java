package com.library.loan_service.model;

public enum LoanPolicy {

    STUDENT(3, 7, 5),
    TEACHER(5, 15, 5),
    EXTERNAL(1, 5, 1);

    private final int maxBooks;
    private final int loanDays;
    private final int maxRenewals;

    LoanPolicy(int maxBooks, int loanDays, int maxRenewals) {
        this.maxBooks = maxBooks;
        this.loanDays = loanDays;
        this.maxRenewals = maxRenewals;
    }

    public int getMaxBooks() {
        return maxBooks;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public int getMaxRenewals() {
        return maxRenewals;
    }

    public static LoanPolicy fromUserType(String userType) {
        return LoanPolicy.valueOf(userType);
    }
}