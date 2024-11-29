package com.ead.lib.moongetter.android.robot.models.builder

import android.content.Context
import com.ead.lib.moongetter.android.robot.models.RobotForAndroid
import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

class Config (
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    class Builder() {


        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null


        /**
         * Initializes a new Request.Builder object with the provided context.
         *
         * Params:
         *
         * context - The context of the application.
         *
         * Returns:
         *
         * The Request.Builder object.
         */
        fun onContext(context: Context) : Builder = apply {
            this.context = context

            return this
        }

        /**
         * Builds a new Request object.
         */
        fun build() : Robot {
            return RobotForAndroid(context?: throw InvalidServerException("Context hasn't provided", Error.INVALID_BUILDER_PARAMETERS))
        }
    }
}