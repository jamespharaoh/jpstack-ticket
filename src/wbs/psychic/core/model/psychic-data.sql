SELECT affiliate_type_insert (
	'psychic_affiliate',
	'default',
	'Psychic affiliate');

SELECT priv_type_insert (
	'psychic',
	'manage',
	'Full control of psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'supervisor',
	'Supervisor',
	'View supervisor reports for this psychic service',
	true);

SELECT priv_type_insert (
	'psychic',
	'stats',
	'View stats for psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'number',
	'View phone numbers for users of psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'messages',
	'View message history for psychic service',
	'',
	true);

SELECT priv_type_insert (
	'slice',
	'psychic_create',
	'Create new psychic services',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'psychic_supervisor',
	'View supervisor screens for psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'psychic_user_view',
	'View all users for this psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'psychic_user_admin',
	'Perform admin on users of this psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic',
	'psychic_user_credit',
	'Perform credit operations on users of this psychic service',
	'',
	true);

SELECT priv_type_insert (
	'psychic_affiliate_group',
	'supervisor',
	'Supervisor',
	'View supervisor reports for this psychic affiliate group',
	true);

SELECT priv_type_insert (
	'psychic_affiliate_group',
	'messages',
	'View message history for this affiliate group',
	'',
	true);

SELECT priv_type_insert (
	'psychic_affiliate',
	'messages',
	'View message history for this affiliate',
	'',
	true);

SELECT priv_type_insert (
	'psychic_affiliate',
	'stats',
	'View message stats for this psychic affiliate',
	'',
	true);

---------------------------------------- INSERT command_type

SELECT command_type_insert (
	'psychic',
	'shortcode',
	'Shortcode');

SELECT command_type_insert (
	'psychic',
	'charges',
	'Confirm charges');

SELECT command_type_insert (
	'psychic',
	'send_to_profile',
	'Send a request to a specific profile');

SELECT command_type_insert (
	'psychic',
	'help',
	'Send a help request to be answered by a staff member');

SELECT command_type_insert (
	'psychic',
	'stop',
	'Stop this service until a further inbound request is made');

---------------------------------------- INSERT queue_type

SELECT queue_type_insert (
	'psychic_affiliate_group',
	'request',
	'Psychic requests pending for this affiliate group',
	'psychic_contact',
	'psychic_request');

SELECT queue_type_insert (
	'psychic',
	'help',
	'Help requests pending for this psychic service',
	'psychic_user',
	'psychic_help_request');

---------------------------------------- INSERT service_type

SELECT service_type_insert (
	'psychic',
	'default',
	'Default');

SELECT service_type_insert (
	'psychic',
	'profile',
	'Psychic profiles');

SELECT service_type_insert (
	'psychic',
	'request',
	'Requests to and responses from profiles');

SELECT service_type_insert (
	'psychic',
	'help',
	'Requests to and responses from help');

SELECT service_type_insert (
	'psychic',
	'bill',
	'Billed messages');

---------------------------------------- INSERT delivery_type

SELECT delivery_type_insert (
	'psychic_bill',
	'Psychic billed messages');

---------------------------------------- INSERT event_type

SELECT event_type_insert (
	'psychic_user_stopped',
	'%0 was stopped by %1');

SELECT event_type_insert (
	'psychic_user_unstopped',
	'%0 was unstopped by %1');
