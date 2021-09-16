## Prerequisites
* create workload identity gke service account (it is mandatory that bootstrap script will be running from SA)
* kubernetes job should be create in namespace that is free from istio-injection (job will be in RUNNING state indefenetly with side-car container)
* set all required ENV variables, they are listed in job.yml under env directive

## Verify
Schema service bootstrap is based on python bootstrap scripts at schema service repository -> `https://community.opengroup.org/osdu/platform/system/schema-service/-/tree/master/deployments/scripts`

Boostrap scripts contains python script which executes clean-up on Datastore to prevent incorect bootstrap for schema service.

After execution of bootstrap-script, you could go to **GCP console** and look for logs under `Kubernetes Engine -> Workloads -> bootstrap-schema pod`
Successful execution will lead to similar output (disclaimer: output could be different due to changes at python3 bootstrap scripts)
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

Additionally new **Datastore Entities** should be created:
Go to `Datastore -> Entitites -> Namespace (dataecosystem) -> Kind (schema)`, this kind should be populated with schema records.
 
