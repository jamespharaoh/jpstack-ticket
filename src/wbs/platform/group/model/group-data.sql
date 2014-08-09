
SELECT object_type_insert (
	'group',
	'group',
	'slice',
	1);

SELECT priv_type_insert (
	'slice',
	'group_create',
	'Create group',
	'Create new groups in this slice',
	true);

SELECT priv_type_insert (
	'group',
	'manage',
	'Manage',
	'Full control',
	true);