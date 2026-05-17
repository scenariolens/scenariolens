# Payment Service Example

This is the reference example project for ScenarioLens.

It contains a realistic `PaymentService` with four methods of increasing complexity, and a deliberately incomplete `PaymentServiceTest` — designed to showcase the gaps ScenarioLens detects.

## Run the analysis

From this directory:

```bash
mvn scenariolens:analyze -DtargetPackage=com.example.payment
```

The report is written to `target/scenariolens/report.html`.

A pre-generated copy is available at [docs/sample-report/report.html](../../docs/sample-report/report.html).

## What's in here

### Source (`src/main/java/com/example/payment/`)

| Class | Description |
|---|---|
| `PaymentService` | Core service — `processRefund`, `processRefundWithRetry`, `transfer`, `getRefundPolicy` |
| `OrderService` | Secondary service — `processOrder`, `cancelOrder`, `checkStatus` |
| `Order`, `RefundRequest`, `PaymentResponse`, … | Domain model and DTOs |

### Tests (`src/test/java/com/example/payment/`)

| Class | Covers | Intentional gaps |
|---|---|---|
| `PaymentServiceTest` | Happy path for `processRefund` | Missing: null order, throws on refund, retry exhausted paths |
| `OrderServiceTest` | `processOrder` happy path | Missing: inventory reservation failure, null order |

## Key gaps ScenarioLens finds

```
[MISSING] processRefund — order=null path
  Stub: orderRepository.findById returns null
  Expected: OrderNotFoundException thrown
  Risk: HIGH — exception path never tested

[MISSING] processRefund — refund throws PaymentException
  Stub: paymentClient.refund throws RuntimeException
  Expected: auditLogger.log called, RefundResponse.failed returned
  Risk: HIGH — catch block untested

[MISSING] transfer — amount exceeds limit
  Stub: policyService.getRefundPolicy returns CANCELLED threshold
  Expected: InvalidStateException thrown
```
