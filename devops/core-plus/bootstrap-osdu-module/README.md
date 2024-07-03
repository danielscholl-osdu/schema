# Verification

Schema service bootstrap is based on python bootstrap scripts at Schema service repository -> `https://community.opengroup.org/osdu/platform/system/schema-service/-/tree/master/deployments/scripts`.

Successful execution will lead to similar output:

> Note: output might be different due to changes in python3 bootstrap scripts.

```
The kind osdu:wks:work-product-component--WellboreTrajectory:1.0.0 was registered successfully.
Try POST for id: osdu:wks:work-product-component--WellboreTrajectory:1.1.0
The kind osdu:wks:work-product-component--WellboreTrajectory:1.1.0 was registered successfully.
Try POST for id: osdu:wks:reference-data--WellboreTrajectoryType:1.0.0
The kind osdu:wks:reference-data--WellboreTrajectoryType:1.0.0 was registered successfully.
Try POST for id: osdu:wks:reference-data--WordFormatType:1.0.0
The kind osdu:wks:reference-data--WordFormatType:1.0.0 was registered successfully.
Try POST for id: osdu:wks:work-product--WorkProduct:1.0.0
The kind osdu:wks:work-product--WorkProduct:1.0.0 was registered successfully.
This update took 156.52 seconds.
All 216 schemas registered, updated or left unchanged because of status PUBLISHED.
```
