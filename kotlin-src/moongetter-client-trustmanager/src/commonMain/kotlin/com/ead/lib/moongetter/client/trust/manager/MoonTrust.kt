package com.ead.lib.moongetter.client.trust.manager

interface MoonTrust {
   interface Manager {
       fun disableCertificationConnections(forceDisable: Boolean = false)

       companion object {
           fun newEmptyFactory() = object : Manager {
               override fun disableCertificationConnections(forceDisable: Boolean) = Unit
           }
       }
   }
}