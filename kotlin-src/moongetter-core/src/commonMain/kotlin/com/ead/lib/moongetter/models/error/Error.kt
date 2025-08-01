package com.ead.lib.moongetter.models.error

enum class Error {

    /**
     * The parameters implemented server working process not valid or empty
     */

    INVALID_PROCESS_IN_EXPECTED_URL_ENTRY,

    /**
     * The parameters implemented in Builder are not valid or empty
     */

    INVALID_BUILDER_PARAMETERS,

    /**
     * Parameters implemented in get() functions are not valid or empty
     */

    NO_PARAMETERS_TO_WORK,

    /**
     * The response is empty or null
     */

    EMPTY_OR_NULL_RESPONSE,


    TIMEOUT_ERROR,

    /**
     * The response is not as expected for finding process as regex or scrapping
     * It is possible that the structure of the target site changed or the request
     * was taken down
     */

    EXPECTED_RESPONSE_NOT_FOUND,

    /**
     * The response is not as expected for finding process for js packed functions
     */

    EXPECTED_PACKED_RESPONSE_NOT_FOUND,

    /**
     * The response is not successful use code : Int? property to handle exception
     */

    UNSUCCESSFUL_RESPONSE,

    /**
     * The resource was definitely taken down
     * another case of resource was taken down
     * check EXPECTED_RESPONSE_NOT_FOUND or EXPECTED_PACKED_RESPONSE_NOT_FOUND
     */

    RESOURCE_TAKEN_DOWN,
}