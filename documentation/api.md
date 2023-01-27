---
description: API endpoints documentation
---

# API


## GET /fields
The following endpoint returns the fields configuration needed by the frontend (JeMPI-UI) in order to properly display patient record data according to a specific 
implementation. This endpoints returns a JSON array. Below a sample of the response :
```json
[
  {
    "fieldName": "type",
    "accessLevel": [],
    "fieldLabel": "Record Type",
    "scope": [
      "/patient/:uid",
      "/match-details",
      "/patient/:uid/linked-records"
    ],
    "groups": [
      "demographics",
      "linked_records"
    ],
    "fieldType": "String"
  },
  ...
]
```

For each field we have a set of attributes, as defined below :

| Attribute   | Description                                                                                                     | Used by            |
|-------------|-----------------------------------------------------------------------------------------------------------------|--------------------|
| fieldName   | A "camel-case" field name which will be used when accessing a patient record data structure                       | Backend + Frontend |
| fieldLabel  | A string that is a human readable name for the field                                                            | Frontend           |
| scope       | Array of URL paths that tells the frontend UI in which pages should the field appear                            | Frontend           |
| groups      | Array of strings which identifies in which section within a frontend UI page should the field be displayed      | Frontend           |
| FieldType   | A string that identifies the type of field, could be String, Date, ...(useful for formatting for example)       | Frontend + Backend |
| accessLevel | An array of string that identifies which user roles are permitted to access a given field (NOT YET IMPLEMENTED) | Frontend           |
| readOnly    | Tells if the field can be editable.                                                                             | Frontend           |

The fields should be configured in the json file for each implementation and should not be updated in production : `JeMPI_Apps/JeMPI_Configuration/config-reference.json`

There's two type of fields : 
- Custom fields : Indexed by the key "fields", contains all the fields that are specific to the implementation. Examples : givenName, nationalId, ...
- System fields : Indexed by the key "systemFields", it contains all the fields that are readonly fields and do not change across the implementation. Example : uid, record type, score, ...

> ! IMPORTANT : The `fieldName` in `config-reference.json` should be set in snake-case, but it's returned in camel-case by the API. 