# UI Component Inventory - HMS Frontend

## Overview

The HMS Frontend uses **shadcn/ui** (Radix UI primitives) with 100+ reusable components organized by feature domains.

**Total Components:** 100+  
**UI Library:** shadcn/ui + Radix UI  
**Styling:** Tailwind CSS 4  
**Type Safety:** TypeScript 5

---

## Component Organization

```
components/
├── ui/              # Base UI primitives (40+ Radix components)
├── appointment/     # Appointment & scheduling components
├── patients/        # Patient management components
├── medical-exam/    # Medical examination components
├── billing/         # Billing & payment components
├── hr/              # HR & employee components
├── lab/             # Laboratory components
├── nurse/           # Nurse-specific components
├── reports/         # Reporting & analytics components
├── auth/            # Authentication components
├── shared/          # Shared/common components
└── landing/         # Marketing/landing page components
```

---

## 1. Base UI Components (`/ui`)

### Layout & Structure
- **Card** - Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter
- **Accordion** - Collapsible content sections
- **Tabs** - Tabbed interfaces
- **Separator** - Visual dividers
- **AspectRatio** - Maintain aspect ratios
- **ResizablePanels** - Resizable layout panels
- **ScrollArea** - Custom scrollable areas

### Forms & Inputs
- **Form** - Form, FormField, FormItem, FormLabel, FormControl, FormDescription, FormMessage
- **Input** - Text input field
- **Textarea** - Multi-line text input
- **Select** - Dropdown select
- **Checkbox** - Checkbox input
- **RadioGroup** - Radio button group
- **Switch** - Toggle switch
- **Slider** - Range slider
- **Calendar** - Date picker calendar
- **DatePicker** - Date selection component
- **InputOTP** - OTP input fields

### Buttons & Actions
- **Button** - Primary action button with variants
- **DropdownMenu** - Context menus and dropdowns
- **ContextMenu** - Right-click context menus
- **Menubar** - Application menu bar
- **NavigationMenu** - Site navigation
- **InlineActions** - Inline action buttons
- **DataTableRowActions** - Table row action menu

### Data Display
- **Table** - Table, TableHeader, TableBody, TableRow, TableCell
- **DataTable** - Enhanced data table with sorting, filtering, pagination
- **DataTablePagination** - Table pagination controls
- **Badge** - Status badges and tags
- **Avatar** - User avatar display
- **Tooltip** - Hover tooltips
- **HoverCard** - Hover preview cards
- **InfoItem** - Key-value information display
- **InfoGrid** - Grid layout for info items
- **EmptyState** - Empty data state
- **EmptyValue** - Display for missing values
- **Chart** - Recharts wrapper components

### Feedback & Overlays
- **Dialog** - Modal dialogs
- **AlertDialog** - Confirmation dialogs
- **Drawer** - Side drawers
- **Sheet** - Side sheets
- **Popover** - Floating popovers
- **Toast** - Toast notifications (Sonner)
- **Alert** - Alert messages
- **AlertBanner** - Banner alerts
- **Progress** - Progress bars
- **Skeleton** - Loading skeletons

### Navigation
- **Breadcrumb** - Breadcrumb navigation
- **Command** - Command palette (⌘K)
- **DetailPageHeader** - Page header with breadcrumbs & actions

### Custom Display Components
- **BloodTypeBadge** - Display blood type
- **GenderBadge** - Display gender
- **CurrencyDisplay** - Format currency
- **FilterPills** - Active filter chips
- **AccountSearchSelect** - Search accounts

---

## 2. Appointment Components (`/appointment`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **AppointmentListShared** | Role-based appointment list | Filtering, pagination, role-specific views |
| **AppointmentCard** | Appointment summary card | Status display, quick actions |
| **AppointmentCalendar** | Calendar view for appointments | Date selection, appointment overlay |
| **AppointmentScheduleView** | Weekly/daily schedule view | Time slots, doctor schedules |
| **TimeSlotPicker** | Available time slot selector | Real-time availability check |
| **DoctorSearchSelect** | Search and select doctor | Async search, department filter |
| **PatientSearchSelect** | Search and select patient | Async search, patient info |
| **AppointmentSearchSelect** | Search existing appointments | Quick appointment lookup |
| **CancelAppointmentModal** | Cancel appointment dialog | Reason input, confirmation |
| **CompleteAppointmentModal** | Mark appointment complete | Status update, notes |
| **AppointmentColumnsShared** | Table column definitions | Role-based columns, actions |

---

## 3. Patient Components (`/patients`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **PatientAvatar** | Patient profile picture | Fallback to initials, upload support |
| **PatientSearchSelect** | Search patient by name/ID | Autocomplete, patient details |
| **BloodTypeBadge** | Display blood type | Color-coded badges |
| **GenderBadge** | Display gender icon | Male/Female/Other icons |
| **AllergyTags** | Display patient allergies | Tag list, highlighting |

---

## 4. Medical Exam Components (`/medical-exam`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **VitalsPanel** | Display vital signs | BP, HR, Temp, Weight, Height |
| **VitalsForm** | Input vital signs form | Validation, unit display |

---

## 5. Billing Components (`/billing`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **InvoiceSummaryCard** | Invoice overview card | Total, paid, balance display |
| **PaymentHistoryTable** | Payment transaction history | Date, method, amount, status |
| **PaymentStatusBadge** | Payment status indicator | Color-coded: Paid, Pending, Failed |
| **PaymentMethodBadge** | Payment method display | Cash, VNPay, Card icons |
| **CurrencyDisplay** | Format VND currency | Thousand separators, đ symbol |

---

## 6. Laboratory Components (`/lab`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **LabSection** | Lab test & results container | Tabs for orders and results |
| **LabOrdersSection** | Lab order list & creation | Order lab tests, status tracking |
| **LabResultsSection** | Lab results display | Results table, image viewing |
| **LabResultsList** | List of lab results | Filterable, sortable table |
| **OrderLabTestDialog** | Create new lab test | Test selection, pricing |
| **OrderLabOrderDialog** | Order tests for patient | Multi-test selection |

---

## 7. HR Components (`/hr`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **DepartmentSelect** | Select department | Dropdown, search |

---

## 8. Nurse Components (`/nurse`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **VitalSignsDialog** | Enter vital signs dialog | Form validation, save to exam |

---

## 9. Reports Components (`/reports`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **RetryButton** | Retry failed data fetch | Error recovery |
| **EmptyReportState** | No data placeholder | Illustration, instructions |
| **CacheInfoBanner** | Cache timestamp display | Last generated time |

---

## 10. Shared Components (`/shared`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **StandardFilterBar** | Reusable filter toolbar | Search, filters, action buttons |
| **StandardRowActions** | Reusable row actions | Edit, delete, custom actions |

---

## 11. Authentication Components (`/auth`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **RoleGuard** | Role-based access control | Wrap components, redirect unauthorized |

---

## 12. Landing Page Components (`/landing`)

| Component | Description | Key Features |
|-----------|-------------|--------------|
| **Header** | Site header with nav | Logo, menu, login/signup buttons |
| **HeroSection** | Landing page hero | CTA buttons, imagery |
| **FeaturesSection** | Features showcase | Icon cards, descriptions |
| **FeatureCard** | Individual feature card | Icon, title, description |
| **Footer** | Site footer | Links, copyright, social |

---

## Component Patterns & Standards

### Naming Conventions
- **PascalCase** for component files and exports
- **Descriptive names** indicating purpose (e.g., `PatientSearchSelect`)
- **Feature prefixes** for domain-specific components

### Common Props Patterns

```typescript
// Search/Select components
interface SearchSelectProps {
  value?: string | number;
  onValueChange: (value: string | number) => void;
  placeholder?: string;
  disabled?: boolean;
}

// Badge components
interface BadgeProps {
  status: StatusEnum;
  size?: "sm" | "md" | "lg";
  className?: string;
}

// Modal/Dialog components
interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
  data?: any;
}
```

### Accessibility
- All components use Radix UI primitives (WAI-ARIA compliant)
- Keyboard navigation support
- Screen reader friendly
- Focus management

### Styling Approach
- **Tailwind utility classes** for styling
- **CVA (class-variance-authority)** for variant management
- **cn() utility** for conditional classNames
- **Consistent color scheme** via design tokens

---

## State Management Patterns

### TanStack Query (React Query)
- Used for server state management
- Caching, refetching, optimistic updates
- Loading and error states handled

### React Hook Form
- Form state and validation
- Integrated with Zod schemas
- Error message display

### React Context
- **AuthContext** - User authentication state
- Role-based rendering

---

## Component Composition Examples

### Data Table Pattern
```tsx
<DataTable
  columns={columns}
  data={data}
  pagination={pagination}
  onPaginationChange={setPagination}
/>
```

### Form Pattern
```tsx
<Form {...form}>
  <FormField
    control={form.control}
    name="fieldName"
    render={({ field }) => (
      <FormItem>
        <FormLabel>Label</FormLabel>
        <FormControl>
          <Input {...field} />
        </FormControl>
        <FormMessage />
      </FormItem>
    )}
  />
</Form>
```

### Dialog Pattern
```tsx
<Dialog open={open} onOpenChange={setOpen}>
  <DialogTrigger>Open</DialogTrigger>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>Title</DialogTitle>
    </DialogHeader>
    {/* Content */}
  </DialogContent>
</Dialog>
```

---

## Reusability Score

| Component Type | Reusability | Count |
|----------------|-------------|-------|
| **Base UI (shadcn)** | Very High | 40+ |
| **Custom Shared** | High | 10+ |
| **Feature-Specific** | Medium | 50+ |
| **Page-Specific** | Low | 10+ |

**Total:** 110+ components

---

## Design System Integration

- **Icons:** Lucide React (consistent icon set)
- **Colors:** Tailwind palette with semantic naming
- **Typography:** System font stack, responsive sizing
- **Spacing:** Tailwind spacing scale (0.25rem increments)
- **Breakpoints:** sm (640px), md (768px), lg (1024px), xl (1280px)

---

## Future Component Additions

- **Notification Center** - Real-time notifications
- **Chat Widget** - Patient-doctor messaging
- **File Upload** - Drag-and-drop file uploads
- **Signature Pad** - Digital signatures
- **Barcode Scanner** - Medicine/patient ID scanning

---

## Summary

- **100+ components** organized by domain
- **40+ Radix UI base components** from shadcn/ui
- **Role-based component libraries** (Admin, Doctor, Nurse, Patient)
- **Consistent design patterns** and accessibility
- **Type-safe** with TypeScript
- **Responsive** and mobile-friendly
- **Performance optimized** with React 19
