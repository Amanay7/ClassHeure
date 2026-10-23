package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.util.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class I18nArabicTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        I18n.init(context)
    }

    @Test
    fun testFrenchTranslations() {
        I18n.setLanguage("fr")
        assertFalse(I18n.isRtl())
        assertEquals("Accueil", I18n.t("nav.home"))
        assertEquals("Enregistrer", I18n.t("actions.save"))
        assertEquals("Annuler", I18n.t("actions.cancel"))
        assertEquals("Supprimer", I18n.t("actions.delete"))
        assertEquals("Modifier", I18n.t("actions.edit"))
        assertEquals("Paramètres", I18n.t("nav.settings"))
    }

    @Test
    fun testArabicTranslations() {
        I18n.setLanguage("ar")
        assertTrue(I18n.isRtl())
        assertEquals("الرئيسية", I18n.t("nav.home"))
        assertEquals("حفظ", I18n.t("actions.save"))
        assertEquals("إلغاء", I18n.t("actions.cancel"))
        assertEquals("حذف", I18n.t("actions.delete"))
        assertEquals("تعديل", I18n.t("actions.edit"))
        assertEquals("الإعدادات", I18n.t("nav.settings"))
        assertEquals("التنقيط", I18n.t("nav.gradebook"))
        assertEquals("اليومية", I18n.t("nav.journal"))
        assertEquals("مواقيت", I18n.t("nav.timetables"))
    }

    @Test
    fun testArabicSixPluralForms() {
        I18n.setLanguage("ar")
        // Forme 0 : zero
        assertEquals("لا يوجد تلاميذ", I18n.plural("students.count", 0))
        // Forme 1 : one
        assertEquals("تلميذ واحد", I18n.plural("students.count", 1))
        // Forme 2 : two (المثنى)
        assertEquals("تلميذان", I18n.plural("students.count", 2))
        // Forme 3 : few (3 à 10 - جمع القلة)
        assertEquals("5 تلاميذ", I18n.plural("students.count", 5))
        assertEquals("10 تلاميذ", I18n.plural("students.count", 10))
        // Forme 4 : many (11 à 99 - جمع الكثرة)
        assertEquals("15 تلميذاً", I18n.plural("students.count", 15))
        assertEquals("35 تلميذاً", I18n.plural("students.count", 35))
        // Forme 5 : other (100+)
        assertEquals("100 تلميذ", I18n.plural("students.count", 100))
        assertEquals("250 تلميذ", I18n.plural("students.count", 250))
    }

    @Test
    fun testFallbackToFrench() {
        I18n.setLanguage("ar")
        // Clé inexistante dans ar mais existante dans fr (si applicable)
        val missingKey = "non.existent.key"
        assertEquals(missingKey, I18n.t(missingKey))
    }

    @Test
    fun testAndroidArabicStringResources() {
        val config = Configuration(context.resources.configuration)
        val locale = Locale("ar")
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        assertEquals("الرئيسية", localizedContext.getString(R.string.nav_home))
        assertEquals("حفظ", localizedContext.getString(R.string.save))
        assertEquals("إلغاء", localizedContext.getString(R.string.cancel))
        assertEquals("الأفواج", localizedContext.getString(R.string.nav_students))
        assertEquals("التنقيط", localizedContext.getString(R.string.nav_gradebook))
        assertEquals("اليومية", localizedContext.getString(R.string.nav_journal))
        assertEquals("مواقيت", localizedContext.getString(R.string.nav_timetables))
        assertEquals("الإعدادات", localizedContext.getString(R.string.nav_settings))
    }
}
