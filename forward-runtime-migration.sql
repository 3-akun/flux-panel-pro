-- Forward runtime config migration
-- Execute once on existing installations before upgrading backend.

ALTER TABLE `forward`
  ADD COLUMN `auto_switch_enabled` int(10) NOT NULL DEFAULT '1' AFTER `strategy`,
  ADD COLUMN `auto_switch_fail_threshold` int(10) NOT NULL DEFAULT '2' AFTER `auto_switch_enabled`,
  ADD COLUMN `auto_switch_recover_threshold` int(10) NOT NULL DEFAULT '3' AFTER `auto_switch_fail_threshold`,
  ADD COLUMN `health_check_interval_sec` int(10) NOT NULL DEFAULT '15' AFTER `auto_switch_recover_threshold`,
  ADD COLUMN `health_check_timeout_ms` int(10) NOT NULL DEFAULT '3000' AFTER `health_check_interval_sec`,
  ADD COLUMN `preferred_target` varchar(255) DEFAULT NULL AFTER `health_check_timeout_ms`;
