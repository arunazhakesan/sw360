/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.licenses
namespace php sw360.thrift.licenses

typedef users.User User
typedef users.RequestedAction RequestedAction
typedef sw360.RequestStatus RequestStatus
typedef sw360.DocumentState DocumentState
typedef sw360.CustomProperties CustomProperties
typedef sw360.RequestSummary RequestSummary
typedef sw360.Ternary Ternary

struct LicenseObligation {
	1: optional string id,
    2: optional string revision
    3: optional string type = "licenseObligation",
    5: required string name,
    6: required i32 obligationId,
}

enum ObligationType {
    ORGANISATION_OBLIGATION = 0,
    PRODUCT_OBLIGATION = 1,
    COMPONENT_OBLIGATION = 2,
}

struct Obligations {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "obligations",
    4: required string text,
    5: optional set<string> whitelist,
    6: optional bool development,
    7: optional bool distribution,
    8: optional list<LicenseObligation> listOfobligation,
    9: optional set<string> obligationDatabaseIds,
    10: optional string title,
    11: optional map<string, string> customPropertyToValue,

    // These two are a quick fix to receiving booleans in PHP not working at the moment
    15: optional string developmentString,
    16: optional string distributionString,

    // information from external data sources
    19: optional map<string, string> externalIds,
    // is valid for Projects
    20: optional bool validForProject,
    21: optional string comments,
    22: optional ObligationType obligationType,
    300: optional map<string, string> additionalData,

}

struct RiskCategory {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "riskCategory",
    5: required i32 riskCategoryId,
    6: required string text
}

struct Risk {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "risk",
    5: required i32 riskId,
    6: required string text,
    7: optional RiskCategory category,
    8: optional string riskCategoryDatabaseId,
}

struct LicenseType {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "licenseType",
	5: required i32 licenseTypeId,
    6: required string licenseType;
}

struct License {
	 1: optional string id,
	 2: optional string revision,
	 3: optional string type = "license",
	 4: optional string shortname, // Short name of the license
	 5: required string fullname,
	 6: optional LicenseType licenseType,
	 7: optional string licenseTypeDatabaseId,
     8: optional string externalLicenseLink,

    // information from external data sources
     9: optional map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    // Additional informations
	// 10: optional bool GPLv2Compat,
	// 11: optional bool GPLv3Compat,
	12: optional string reviewdate,

	15: optional Ternary GPLv2Compat = sw360.Ternary.UNDEFINED,
	16: optional Ternary GPLv3Compat = sw360.Ternary.UNDEFINED,

    20: optional list<Obligations> obligations,
    21: optional set<string> obligationDatabaseIds,
	22: optional list<Risk> risks,
	23: optional set<string> riskDatabaseIds,
    25: optional string text,

    30: optional bool checked = true;

    90: optional DocumentState documentState,

	200: optional map<RequestedAction, bool> permissions
}

service LicenseService {

    /**
     * Get a single license by providing its ID, filled with license type, risks and obligations containing obligations and whitelists
     * filtered for the given organisation
     **/
    License getByID(1:string id, 2: string organisation);

    /**
      * Get a single license by providing its ID, filled with license type and obligations containing obligations and whitelists
      * filtered for the given organisation, risks are not set,
      * user's moderation request with status pending or in progress applied
      **/
    License getByIDWithOwnModerationRequests(1:string id, 2: string organisation, 3: User user);

    /**
     * get licenses for ids filled with license types, risks and obligations containing obligations
     * whitelists filtered for organisation
     **/
    list<License> getByIds(1:set<string> ids, 2: string organisation);

    /**
     * Add a new obligation object to database, return id
     **/
    string addObligations(1:Obligations obligations, 2: User user);

    /**
    * Add an existing obligation to a license or generate moderation request if user has no permission
    **/
    RequestStatus addObligationsToLicense(1: Obligations obligations, 2: string licenseId, 3: User user);

    /**
     * Update given license,
     * user is used to check permissions, requesting user's department is used to set whitelists
     **/
    RequestStatus updateLicense(1: License license, 2: User user, 3: User requestingUser);

     /**
      * update license in database if user has permissions, additions and deletions are the parts of the moderation request
      * that specify which properties to add to and which to delete from license
      **/
    RequestStatus updateLicenseFromModerationRequest(1: License additions, 2: License deletions, 3: User user, 4: User requestingUser);

    /**
     * Update the whitelisted obligations for an organisation, generate moderation request if user has no permissions
     **/
    RequestStatus updateWhitelist(1: string licenseId, 2: set<string> obligationsDatabaseIds, 3: User user);

    /**
     * delete license from database if user has permissions,
     * otherwise create moderation request
     **/
    RequestStatus deleteLicense(1: string licenseId, 2: User user);

    /**
     * Get a list of summaries filled with license types for all licenses
     **/
    list<License> getLicenseSummary();

    /**
     * Get a list of export summaries for all licenses
     **/
    list<License> getLicenseSummaryForExport();

    /**
     * get a list of all full license documents filled with obligations, risks and license types,
     * obligations and risks themselves are not filled
     **/
    list<License> getDetailedLicenseSummaryForExport(1: string organisation);

    /**
      * get a list of full documents with ids in identifiers, filled with obligations, risks and license types,
      * obligations and risks themselves are also filled, obligation whitelists are filtered for organisation
      **/
    list<License> getDetailedLicenseSummary(1: string organisation, 2: list<string> identifiers);

    /**
     * bulk add for import of license archive, returns input risk categories if successful, null otherwise
     **/
    list<RiskCategory> addRiskCategories(1: list <RiskCategory> riskCategories, 2: User user);

    /**
     * bulk add for import of license archive, returns input risks if successful, null otherwise
     **/
    list<Risk> addRisks(1: list <Risk> risks, 2: User user);

    /**
     * bulk add for import of license archive, returns input obligations if successful, null otherwise
     **/
    list<LicenseObligation> addListOfobligation(1: list <LicenseObligation> obligations, 2: User user);

    /**
     * bulk add for import of license archive, returns input license types if successful, null otherwise
     **/
    list<LicenseType> addLicenseTypes(1: list <LicenseType> licenseTypes, 2: User user);

    /**
     * bulk add for import of license archive, returns input licenses if successful, null otherwise
     **/
    list<License> addLicenses(1: list <License> licenses, 2: User user);

    /**
     * bulk add for import of license archive, returns input licenses if successful, null otherwise
     **/
    list<License> addOrOverwriteLicenses(1: list <License> licenses, 2: User user);

    /**
     * bulk add for import of license archive, returns input obligations if successful, null otherwise
     **/
    list<Obligations> addListOfObligations(1: list <Obligations> obligations, 2: User user);

    /**
     * get complete list of risk categories
     **/
    list<RiskCategory> getRiskCategories();

    /**
     * get complete list of filled risks
     **/
    list<Risk> getRisks();

    /**
     * get complete list of license types
     **/
    list<LicenseType> getLicenseTypes();

    /**
     * get complete list of licenses, completely filled
     **/
    list<License> getLicenses();

    /**
     * get complete list of filled obligations
     **/
    list<Obligations> getObligations();

    /**
     * get complete list of obligations
     **/
    list<LicenseObligation> getListOfobligation();

    /**
    * get filled risks with id in ids
    **/
    list<Risk> getRisksByIds( 1: list<string> ids);

    /**
     * get risk categories with id in ids
     **/
    list<RiskCategory> getRiskCategoriesByIds( 1: list<string> ids);

    /**
     * get obligations with id in ids
     **/
    list<LicenseObligation> getListOfobligationByIds( 1: list<string> ids);

    /**
     * get license types with id in ids
     **/
    list<LicenseType> getLicenseTypesByIds( 1: list<string> ids);

    /**
     * get filled obligations with id in ids
     **/
    list<Obligations> getObligationsByIds( 1: list<string> ids);

    /**
     * return filled risk
     **/
    Risk getRiskById( 1: string id);

    RiskCategory getRiskCategoryById( 1: string id);
    LicenseObligation getObligationById( 1: string id);
    LicenseType getLicenseTypeById( 1: string id);

    /**
     * return filled obligation
     **/
    Obligations getObligationsById( 1: string id);

    list<CustomProperties> getCustomProperties(1: string documentType);

    RequestStatus updateCustomProperties(1: CustomProperties customProperties, 2: User user );

   /**
    * removes all licenses, license types, obligations, obligations, risks, risk categories from db
    **/
    RequestSummary deleteAllLicenseInformation(1: User user);

    RequestSummary importAllSpdxLicenses(1: User user);
    /**
     * delete obligation from database if user has permissions
     **/
    RequestStatus deleteObligations(1: string id, 2: User user);
}
