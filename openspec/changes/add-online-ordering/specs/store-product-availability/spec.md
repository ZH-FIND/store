## ADDED Requirements

### Requirement: 门店停售商品
门店店员 SHALL 能将本门店的某个商品置为停售。停售 MUST 以「门店 × 商品」为粒度，MUST NOT 影响其他门店对该商品的售卖。

#### Scenario: 停售本门店的某商品
- **WHEN** 门店店员将本门店的某个商品置为停售
- **THEN** 该门店的商品列表中该商品 `available` 变为 `false`

#### Scenario: 停售不影响其他门店
- **WHEN** A 门店将某商品置为停售
- **THEN** B 门店的同一商品 `available` 仍为 `true`，且顾客仍可在 B 门店正常下单该商品

### Requirement: 门店恢复商品
门店店员 SHALL 能将本门店已停售的商品恢复为可售。

#### Scenario: 恢复已停售商品
- **WHEN** 门店店员将本门店某已停售商品恢复为可售
- **THEN** 该商品在该门店的 `available` 变为 `true`，顾客侧立即可选

#### Scenario: 恢复本就可售的商品
- **WHEN** 门店店员对某门店下一个本就 `available` 为 `true` 的商品执行恢复
- **THEN** 系统返回成功且该商品仍为可售，不产生异常

### Requirement: 可售状态强制约束下单
系统 MUST 在服务端校验可售状态，停售商品的拦截 MUST NOT 仅依赖前端。

#### Scenario: 绕开前端直接提交停售商品
- **WHEN** 客户端直接调用下单接口，提交某门店下已停售的商品
- **THEN** 系统拒绝该订单，返回 400 并指明不可售的商品，不创建任何订单记录

#### Scenario: 停售后原有订单不受影响
- **WHEN** 某商品被停售
- **THEN** 停售前已经生成的、包含该商品的订单 MUST 保持原有内容与状态不变
