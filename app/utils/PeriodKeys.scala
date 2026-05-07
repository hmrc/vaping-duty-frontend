package utils

import java.time.Month
import java.time.Month._
import java.time.format.TextStyle
import java.util.Locale

object PeriodKeys {
  def fromDisplayName(name: String): Option[Month] =
    Month.values().find(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH) == name)

  def toEtmpMonthString(month: Month): String = month match {
    case JANUARY   => "AA"
    case FEBRUARY  => "AB"
    case MARCH     => "AC"
    case APRIL     => "AD"
    case MAY       => "AE"
    case JUNE      => "AF"
    case JULY      => "AG"
    case AUGUST    => "AH"
    case SEPTEMBER => "AI"
    case OCTOBER   => "AJ"
    case NOVEMBER  => "AK"
    case DECEMBER  => "AL"
  }

  def fromEtmpMonthString(monthKey: String): Option[Month] = monthKey match {
    case "AA" => Some(JANUARY)
    case "AB" => Some(FEBRUARY)
    case "AC" => Some(MARCH)
    case "AD" => Some(APRIL)
    case "AE" => Some(MAY)
    case "AF" => Some(JUNE)
    case "AG" => Some(JULY)
    case "AH" => Some(AUGUST)
    case "AI" => Some(SEPTEMBER)
    case "AJ" => Some(OCTOBER)
    case "AK" => Some(NOVEMBER)
    case "AL" => Some(DECEMBER)
    case _    => None
  }

  def toDisplayName(month: Month): String =
    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
}