package uk.gov.ons.ctp.integration.rhsvc.representation.util;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

/**
 * Orika Bi-directional custom converter to convert String to UniquePropertyReferenceNumber and vice
 * versa.
 */
public class StringToUPRNConverter
    extends BidirectionalConverter<String, UniquePropertyReferenceNumber> {

  /**
   * Converts String to UniquePropertyReferenceNumber
   *
   * @param source String to convert
   * @param destinationType currently unused
   * @param mappingContext currently unused
   * @return UniquePropertyReferenceNumber representation of String
   */
  public UniquePropertyReferenceNumber convertTo(
      String source,
      Type<UniquePropertyReferenceNumber> destinationType,
      MappingContext mappingContext) {
    return new UniquePropertyReferenceNumber(source);
  }

  /**
   * Converts UniquePropertyReferenceNumber to String
   *
   * @param source UniquePropertyReferenceNumber to convert
   * @param destinationType currently unused
   * @param mappingContext currently unused
   * @return String representation of UniquePropertyReferenceNumber value
   */
  public String convertFrom(
      UniquePropertyReferenceNumber source,
      Type<String> destinationType,
      MappingContext mappingContext) {
    return String.valueOf(source.getValue());
  }
}
