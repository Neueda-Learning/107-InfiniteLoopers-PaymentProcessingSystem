import { SectionCard } from '../../components/UI';

export function CustomerSelector({
  customers,
  selectedCustomerId,
  onSelectCustomer,
  loading,
}) {
  return (
    <SectionCard title="Step 1: Select sender" subtitle="Choose who is making this transfer.">
      <label>
        <span>Sender customer</span>
        <select
          value={selectedCustomerId}
          onChange={(event) => onSelectCustomer(event.target.value)}
          disabled={loading}
        >
          <option value="">Select customer</option>
          {customers.map((customer) => (
            <option key={customer.id} value={customer.id}>
              {customer.customerName}
            </option>
          ))}
        </select>
      </label>
    </SectionCard>
  );
}

