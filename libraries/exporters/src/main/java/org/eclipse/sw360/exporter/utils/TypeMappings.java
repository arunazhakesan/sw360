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
package org.eclipse.sw360.exporter.utils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.commons.csv.CSVRecord;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.exporter.utils.ConvertRecord.putToTodos;

/**
 * @author johannes.najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class TypeMappings {

    @NotNull
    public static <T> Predicate<T> containedWithAddIn(final Set<T> knownIds) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return knownIds.add(input);
            }
        };
    }

    public static <T, U> ImmutableList<T> getElementsWithIdentifiersNotInMap(Function<T, U> getIdentifier, Map<U, T> theMap, List<T> candidates) {
        return FluentIterable.from(candidates).filter(
                CommonUtils.afterFunction(getIdentifier).is(containedWithAddIn(Sets.newHashSet(theMap.keySet())))).toList();
    }

    public static <T, U> ImmutableList<T> getElementsWithIdentifiersNotInSet(Function<T, U> getIdentifier, Set<U> theSet, List<T> candidates) {
        return FluentIterable.from(candidates).filter(
                CommonUtils.afterFunction(getIdentifier).is(containedWithAddIn(theSet))).toList();
    }

    @NotNull
    public static Function<License, String> getLicenseIdentifier() {
        return new Function<License, String>() {
            @Override
            public String apply(License input) {
                return input.getId();
            }
        };
    }

    @NotNull
    public static Function<Obligations, Integer> getTodoIdentifier() {
        return new Function<Obligations, Integer>() {
            @Override
            public Integer apply(Obligations input) {
                return -1;
            }
        };
    }

    @NotNull
    public static Function<LicenseObligation, Integer> getObligationIdentifier() {
        return new Function<LicenseObligation, Integer>() {
            @Override
            public Integer apply(LicenseObligation input) {
                return input.getObligationId();
            }
        };
    }

    @NotNull
    public static Function<LicenseType, Integer> getLicenseTypeIdentifier() {
        return new Function<LicenseType, Integer>() {
            @Override
            public Integer apply(LicenseType input) {
                return input.getLicenseTypeId();
            }
        };
    }

    @NotNull
    public static Function<RiskCategory, Integer> getRiskCategoryIdentifier() {
        return new Function<RiskCategory, Integer>() {
            @Override
            public Integer apply(RiskCategory input) {
                return input.getRiskCategoryId();
            }
        };
    }

    @NotNull
    public static Function<Risk, Integer> getRiskIdentifier() {
        return new Function<Risk, Integer>() {
            @Override
            public Integer apply(Risk input) {
                return input.getRiskId();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T, U> Function<T, U> getIdentifier(Class<T> clazz, @SuppressWarnings("unused") Class<U> uClass /*used to infer type*/) throws SW360Exception {
        if (clazz.equals(LicenseType.class)) {
            return (Function<T, U>) getLicenseTypeIdentifier();
        } else if (clazz.equals(LicenseObligation.class)) {
            return (Function<T, U>) getObligationIdentifier();
        } else if (clazz.equals(RiskCategory.class)) {
            return (Function<T, U>) getRiskCategoryIdentifier();
        }

        throw new SW360Exception("Unknown Type requested");
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getAllFromDB(LicenseService.Iface licenseClient, Class<T> clazz) throws TException {
        if (clazz.equals(LicenseType.class)) {
            return (List<T>) licenseClient.getLicenseTypes();
        } else if (clazz.equals(LicenseObligation.class)) {
            return (List<T>) licenseClient.getListOfobligation();
        } else if (clazz.equals(RiskCategory.class)) {
            return (List<T>) licenseClient.getRiskCategories();
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> simpleConvert(List<CSVRecord> records, Class<T> clazz) throws SW360Exception {
        if (clazz.equals(LicenseType.class)) {
            return (List<T>) ConvertRecord.convertLicenseTypes(records);
        } else if (clazz.equals(LicenseObligation.class)) {
            return (List<T>) ConvertRecord.convertObligation(records);
        } else if (clazz.equals(RiskCategory.class)) {
            return (List<T>) ConvertRecord.convertRiskCategories(records);
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> addAlltoDB(LicenseService.Iface licenseClient, Class<T> clazz, List<T> candidates, User user) throws TException {
        if (candidates != null && !candidates.isEmpty()) {
            if (clazz.equals(LicenseType.class)) {
                return (List<T>) licenseClient.addLicenseTypes((List<LicenseType>) candidates, user);
            } else if (clazz.equals(LicenseObligation.class)) {
                return (List<T>) licenseClient.addListOfobligation((List<LicenseObligation>) candidates, user);
            } else if (clazz.equals(RiskCategory.class)) {
                return (List<T>) licenseClient.addRiskCategories((List<RiskCategory>) candidates, user);
            }
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @NotNull
    public static <T, U> Map<U, T> getIdentifierToTypeMapAndWriteMissingToDatabase(LicenseService.Iface licenseClient, InputStream in, Class<T> clazz, Class<U> uClass, User user) throws TException {
        Map<U, T> typeMap;
        List<CSVRecord> records = ImportCSV.readAsCSVRecords(in);
        final List<T> recordsToAdd = simpleConvert(records, clazz);
        final List<T> fullList = CommonUtils.nullToEmptyList(getAllFromDB(licenseClient, clazz));
        typeMap = Maps.newHashMap(Maps.uniqueIndex(fullList, getIdentifier(clazz, uClass)));
        final ImmutableList<T> filteredList = getElementsWithIdentifiersNotInMap(getIdentifier(clazz, uClass), typeMap, recordsToAdd);
        List<T> added = null;
        if (filteredList.size() > 0) {
            added = addAlltoDB(licenseClient, clazz, filteredList, user);
        }
        if (added != null)
            typeMap.putAll(Maps.uniqueIndex(added, getIdentifier(clazz, uClass)));
        return typeMap;
    }

    @NotNull
    public static Map<Integer, Risk> getIntegerRiskMap(LicenseService.Iface licenseClient, Map<Integer, RiskCategory> riskCategoryMap, InputStream in, User user) throws TException {
        List<CSVRecord> riskRecords = ImportCSV.readAsCSVRecords(in);
        final List<Risk> risksToAdd = ConvertRecord.convertRisks(riskRecords, riskCategoryMap);
        final List<Risk> risks = CommonUtils.nullToEmptyList(licenseClient.getRisks());
        Map<Integer, Risk> riskMap = Maps.newHashMap(Maps.uniqueIndex(risks, getRiskIdentifier()));
        final ImmutableList<Risk> filteredList = getElementsWithIdentifiersNotInMap(getRiskIdentifier(), riskMap, risksToAdd);
        List<Risk> addedRisks = null;
        if (filteredList.size() > 0) {
            addedRisks = licenseClient.addRisks(filteredList, user);
        }
        if (addedRisks != null)
            riskMap.putAll(Maps.uniqueIndex(addedRisks, getRiskIdentifier()));
        return riskMap;
    }

    @NotNull
    public static Map<Integer, Obligations> getTodoMapAndWriteMissingToDatabase(LicenseService.Iface licenseClient, Map<Integer, LicenseObligation> obligationMap, Map<Integer, Set<Integer>> obligationTodoMapping, InputStream in, User user) throws TException {
        List<CSVRecord> obligRecords = ImportCSV.readAsCSVRecords(in);
        final List<Obligations> obligations = CommonUtils.nullToEmptyList(licenseClient.getObligations());
        Map<Integer, Obligations> obligMap = Maps.newHashMap(Maps.uniqueIndex(obligations, getTodoIdentifier()));
        final List<Obligations> obligationsToAdd = ConvertRecord.convertTodos(obligRecords);
        final ImmutableList<Obligations> filteredTodos = getElementsWithIdentifiersNotInMap(getTodoIdentifier(), obligMap, obligationsToAdd);
        final ImmutableMap<Integer, Obligations> filteredMap = Maps.uniqueIndex(filteredTodos, getTodoIdentifier());
        putToTodos(obligationMap, filteredMap, obligationTodoMapping);
        //insertCustomProperties

        if (filteredTodos.size() > 0) {
            final List<Obligations> addedTodos = licenseClient.addListOfObligations(filteredTodos, user);
            if (addedTodos != null) {
                final ImmutableMap<Integer, Obligations> addedTodoMap = Maps.uniqueIndex(addedTodos, getTodoIdentifier());
                obligMap.putAll(addedTodoMap);
            }
        }
        return obligMap;
    }

    @NotNull
    public static Map<Integer, Obligations> updateTodoMapWithCustomPropertiesAndWriteToDatabase(LicenseService.Iface licenseClient, Map<Integer, Obligations> obligMap, Map<Integer, ConvertRecord.PropertyWithValue> customPropertiesMap, Map<Integer, Set<Integer>> obligPropertiesMap, User user) throws TException {
        for(Integer obligId : obligPropertiesMap.keySet()){
            Obligations oblig = obligMap.get(obligId);
            if(! oblig.isSetCustomPropertyToValue()){
                oblig.setCustomPropertyToValue(new HashMap<>());
            }
            for(Integer propertyWithValueId : obligPropertiesMap.get(obligId)){
                ConvertRecord.PropertyWithValue propertyWithValue = customPropertiesMap.get(propertyWithValueId);
                oblig.getCustomPropertyToValue().put(propertyWithValue.getProperty(), propertyWithValue.getValue());
            }
        }

        if (obligMap.values().size() > 0) {
            final List<Obligations> addedTodos = licenseClient.addListOfObligations(obligMap.values().stream().collect(Collectors.toList()), user);
            if (addedTodos != null) {
                final ImmutableMap<Integer, Obligations> addedTodoMap = Maps.uniqueIndex(addedTodos, getTodoIdentifier());
                obligMap.putAll(addedTodoMap);
            }
        }
        return obligMap;
    }

    public static  Map<Integer, ConvertRecord.PropertyWithValue> getCustomPropertiesWithValuesByIdAndWriteMissingToDatabase(LicenseService.Iface licenseClient, InputStream inputStream, User user) throws TException {
        List<CSVRecord> records = ImportCSV.readAsCSVRecords(inputStream);
        Optional<CustomProperties> dbCustomProperties = CommonUtils.wrapThriftOptionalReplacement(licenseClient.getCustomProperties(SW360Constants.TYPE_OBLIGATIONS));
        CustomProperties customProperties;
        if (!dbCustomProperties.isPresent()) {
            customProperties = new CustomProperties().setDocumentType(SW360Constants.TYPE_OBLIGATIONS);
        } else {
            customProperties = dbCustomProperties.get();
        }
        Map<String, Set<String>> propertyToValuesToAdd = ConvertRecord.convertCustomProperties(records);
        customProperties.setPropertyToValues(CommonUtils.mergeMapIntoMap(propertyToValuesToAdd, customProperties.getPropertyToValues()));
        licenseClient.updateCustomProperties(customProperties, user);
        Map<Integer, ConvertRecord.PropertyWithValue> propertiesWithValuesById = ConvertRecord.convertCustomPropertiesById(records);
        return propertiesWithValuesById;
    }
}
