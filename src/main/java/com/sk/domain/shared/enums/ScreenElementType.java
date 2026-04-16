package com.sk.domain.shared.enums;

public enum ScreenElementType {

    MENU_TOP(Category.MENU),
    MENU_ITEM(Category.MENU),
    MENU_GROUP(Category.MENU),
    FLOATING(Category.FLOATING);

    private final Category category;

    ScreenElementType(Category category) {
        this.category = category;
    }

    public boolean isMenu() {
        return this.category == Category.MENU;
    }

    public boolean isFloating() {
        return this.category == Category.FLOATING;
    }

    public enum Category {
        MENU, FLOATING
    }

}
