package com.veyra.core.constants;

/**
 * Tüm API path öneklerini merkezi olarak tanımlar.
 * Controller'lar bu sabitleri kullanır — path değişirse tek yerden güncellenir (DRY).
 * final class — instantiate edilemez.
 */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_V1           = "/api/v1";

    public static final String AUTH             = API_V1 + "/auth";
    public static final String USERS            = API_V1 + "/users";
    public static final String BRANDS           = API_V1 + "/brands";
    public static final String CAR_MODELS       = API_V1 + "/models";
    public static final String CARS             = API_V1 + "/cars";
    public static final String RENTALS          = API_V1 + "/rentals";
    public static final String PAYMENTS         = API_V1 + "/payments";
}
